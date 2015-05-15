/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.lazy

import com.google.common.base.Function
import com.google.common.base.Functions
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.computeTypeInContext
import org.jetbrains.kotlin.cfg.JetFlowInformationProvider
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.di.InjectorForBodyResolve
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public abstract class ElementResolver protected(
        public val resolveSession: ResolveSession
) {

    public open fun getElementAdditionalResolve(jetElement: JetElement): BindingContext {
        return performElementAdditionalResolve(jetElement, jetElement, BodyResolveMode.FULL)
    }

    public open fun hasElementAdditionalResolveCached(jetElement: JetElement): Boolean = false

    protected open fun probablyNothingCallableNames(): ProbablyNothingCallableNames
            = throw UnsupportedOperationException("Cannot use partial body resolve with no Nothing-functions index");

    public fun resolveToElement(jetElement: JetElement, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext {
        var jetElement = jetElement

        val elementOfAdditionalResolve = findElementOfAdditionalResolve(jetElement)

        if (elementOfAdditionalResolve != null) {
            if (elementOfAdditionalResolve !is JetParameter) {
                if (bodyResolveMode != BodyResolveMode.FULL && !hasElementAdditionalResolveCached(elementOfAdditionalResolve)) {
                    return performElementAdditionalResolve(elementOfAdditionalResolve, jetElement, bodyResolveMode)
                }

                return getElementAdditionalResolve(elementOfAdditionalResolve)
            }

            val klass = elementOfAdditionalResolve.getParentOfType<JetClass>(true)
            if (klass != null && elementOfAdditionalResolve.getParent() == klass.getPrimaryConstructorParameterList()) {
                return getElementAdditionalResolve(klass)
            }

            // Parameters for function literal could be met inside other parameters. We can't make resolveToDescriptors for internal elements.
            jetElement = elementOfAdditionalResolve
        }

        val declaration = jetElement.getParentOfType<JetDeclaration>(false)
        if (declaration != null && declaration !is JetClassInitializer) {
            // Activate descriptor resolution
            resolveSession.resolveToDescriptor(declaration)
        }

        return resolveSession.getBindingContext()
    }

    private fun findElementOfAdditionalResolve(element: JetElement): JetElement? {
        var elementOfAdditionalResolve = JetPsiUtil.getTopmostParentOfTypes(
                element,
                javaClass<JetNamedFunction>(),
                javaClass<JetClassInitializer>(),
                javaClass<JetSecondaryConstructor>(),
                javaClass<JetProperty>(),
                javaClass<JetParameter>(),
                javaClass<JetDelegationSpecifierList>(),
                javaClass<JetInitializerList>(),
                javaClass<JetImportDirective>(),
                javaClass<JetAnnotationEntry>(),
                javaClass<JetTypeParameter>(),
                javaClass<JetTypeConstraint>(),
                javaClass<JetPackageDirective>(),
                javaClass<JetCodeFragment>()) as JetElement?

        if (elementOfAdditionalResolve is JetPackageDirective) {
            return element
        }

        return elementOfAdditionalResolve
    }

    protected fun performElementAdditionalResolve(resolveElement: JetElement, contextElement: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
        // All additional resolve should be done to separate trace
        val trace = resolveSession.getStorageManager().createSafeTrace(
                DelegatingBindingTrace(resolveSession.getBindingContext(), "trace to resolve element", resolveElement))

        val file = resolveElement.getContainingJetFile()

        val statementFilter = if (bodyResolveMode != BodyResolveMode.FULL && resolveElement is JetDeclaration)
            PartialBodyResolveFilter(contextElement, resolveElement, probablyNothingCallableNames(), bodyResolveMode == BodyResolveMode.PARTIAL_FOR_COMPLETION)
        else
            StatementFilter.NONE

        when (resolveElement) {
            is JetNamedFunction -> functionAdditionalResolve(resolveSession, resolveElement, trace, file, statementFilter)

            is JetClassInitializer -> initializerAdditionalResolve(resolveSession, resolveElement, trace, file, statementFilter)

            is JetSecondaryConstructor -> secondaryConstructorAdditionalResolve(resolveSession, resolveElement, trace, file, statementFilter)

            is JetProperty -> propertyAdditionalResolve(resolveSession, resolveElement, trace, file, statementFilter)

            is JetDelegationSpecifierList -> delegationSpecifierAdditionalResolve(resolveSession, resolveElement.getParent() as JetClassOrObject, trace, file)

            is JetInitializerList -> delegationSpecifierAdditionalResolve(resolveSession, resolveElement.getParent() as JetEnumEntry, trace, file)

            is JetImportDirective -> {
                val scope = resolveSession.getScopeProvider().getFileScope(resolveElement.getContainingJetFile())
                scope.forceResolveAllImports()
            }

            is JetAnnotationEntry -> annotationAdditionalResolve(resolveSession, resolveElement)

            is JetClass -> constructorAdditionalResolve(resolveSession, resolveElement, trace, file, statementFilter)

            is JetTypeParameter -> typeParameterAdditionalResolve(resolveSession, resolveElement)

            is JetTypeConstraint -> typeConstraintAdditionalResolve(resolveSession, resolveElement)

            is JetCodeFragment -> codeFragmentAdditionalResolve(resolveSession, resolveElement, trace, bodyResolveMode)

            else -> {
                if (resolveElement.getParentOfType<JetPackageDirective>(true) != null) {
                    packageRefAdditionalResolve(resolveSession, trace, resolveElement)
                }
                else {
                    error("Invalid type of the topmost parent: $resolveElement\n${resolveElement.getElementTextWithContext()}")
                }
            }
        }

        JetFlowInformationProvider(resolveElement, trace).checkDeclaration()

        return trace.getBindingContext()
    }

    private fun packageRefAdditionalResolve(resolveSession: ResolveSession, trace: BindingTrace, jetElement: JetElement) {
        if (jetElement is JetSimpleNameExpression) {
            val header = jetElement.getParentOfType<JetPackageDirective>(true)!!

            if (trace.getBindingContext()[BindingContext.RESOLUTION_SCOPE, jetElement] == null) {
                val scope = getExpressionMemberScope(resolveSession, jetElement)
                if (scope != null) {
                    trace.record(BindingContext.RESOLUTION_SCOPE, jetElement, scope)
                }
            }

            if (Name.isValidIdentifier(jetElement.getReferencedName())) {
                if (trace.getBindingContext()[BindingContext.REFERENCE_TARGET, jetElement] == null) {
                    val fqName = header.getFqName(jetElement)
                    val packageDescriptor = resolveSession.getModuleDescriptor().getPackage(fqName)
                                            ?: error("Package descriptor should be present in session for $fqName")
                    trace.record(BindingContext.REFERENCE_TARGET, jetElement, packageDescriptor)
                }
            }
        }
    }

    private fun typeConstraintAdditionalResolve(analyzer: KotlinCodeAnalyzer, jetTypeConstraint: JetTypeConstraint) {
        val declaration = jetTypeConstraint.getParentOfType<JetDeclaration>(true)!!
        val descriptor = analyzer.resolveToDescriptor(declaration) as ClassDescriptor

        for (parameterDescriptor in descriptor.getTypeConstructor().getParameters()) {
            ForceResolveUtil.forceResolveAllContents<TypeParameterDescriptor>(parameterDescriptor)
        }
    }

    private fun codeFragmentAdditionalResolve(resolveSession: ResolveSession, codeFragment: JetCodeFragment, trace: BindingTrace, bodyResolveMode: BodyResolveMode) {
        val codeFragmentExpression = codeFragment.getContentElement() as? JetExpression ?: return
        val contextElement = codeFragment.correctedContext

        val scopeForContextElement: JetScope?
        val dataFlowInfoForContextElement: DataFlowInfo
        when (contextElement) {
            is JetClassOrObject -> {
                val descriptor = resolveSession.resolveToDescriptor(contextElement) as LazyClassDescriptor
                scopeForContextElement = descriptor.getScopeForMemberDeclarationResolution()
                dataFlowInfoForContextElement = DataFlowInfo.EMPTY
            }

            is JetExpression -> {
                // do not use PARTIAL body resolve mode because because it does not know about names used in our fragment
                val contextResolveMode = if (bodyResolveMode == BodyResolveMode.PARTIAL)
                    BodyResolveMode.PARTIAL_FOR_COMPLETION
                else
                    bodyResolveMode
                val contextForElement = resolveToElement(contextElement, contextResolveMode)
                scopeForContextElement = contextForElement[BindingContext.RESOLUTION_SCOPE, contextElement]
                dataFlowInfoForContextElement = contextForElement.getDataFlowInfo(contextElement)
            }

            else -> return
        }

        if (scopeForContextElement == null) return

        val codeFragmentScope = resolveSession.getScopeProvider().getFileScope(codeFragment)
        val chainedScope = ChainedScope(scopeForContextElement.getContainingDeclaration(),
                                        "Scope for resolve code fragment", scopeForContextElement, codeFragmentScope)

        codeFragmentExpression.computeTypeInContext(chainedScope, trace, dataFlowInfoForContextElement,
                                                    TypeUtils.NO_EXPECTED_TYPE, resolveSession.getModuleDescriptor())
    }

    //TODO: this code should be moved into debugger which should set correct context for its code fragment
    private val JetCodeFragment.correctedContext: PsiElement?
        get() {
            val context = getContext()
            if (context is JetBlockExpression) {
                return context.getStatements().filterIsInstance<JetExpression>().lastOrNull() ?: context
            }
            return context
        }

    private fun annotationAdditionalResolve(resolveSession: ResolveSession, jetAnnotationEntry: JetAnnotationEntry) {
        val modifierList = jetAnnotationEntry.getParentOfType<JetModifierList>(true)
        val declaration = modifierList?.getParentOfType<JetDeclaration>(true)
        if (declaration != null) {
            doResolveAnnotations(resolveSession, getAnnotationsByDeclaration(resolveSession, modifierList!!, declaration))
        }
        else {
            val fileAnnotationList = jetAnnotationEntry.getParentOfType<JetFileAnnotationList>(true)
            if (fileAnnotationList != null) {
                doResolveAnnotations(resolveSession, resolveSession.getFileAnnotations(fileAnnotationList.getContainingJetFile()))
            }
            if (modifierList != null && modifierList.getParent() is JetFile) {
                doResolveAnnotations(resolveSession, resolveSession.getDanglingAnnotations(modifierList.getContainingJetFile()))
            }
        }
    }

    private fun doResolveAnnotations(resolveSession: ResolveSession, annotations: Annotations) {
        AnnotationResolver.resolveAnnotationsArguments(annotations, resolveSession.getTrace())
        ForceResolveUtil.forceResolveAllContents(annotations)
    }

    private fun getAnnotationsByDeclaration(resolveSession: ResolveSession, modifierList: JetModifierList, declaration: JetDeclaration): Annotations {
        var descriptor = resolveSession.resolveToDescriptor(declaration)
        if (declaration is JetClass) {
            if (modifierList == declaration.getPrimaryConstructorModifierList()) {
                descriptor = (descriptor as ClassDescriptor).getUnsubstitutedPrimaryConstructor()
                             ?: error("No constructor found: ${declaration.getText()}")
            }
        }

        if (declaration is JetClassOrObject && modifierList.getParent() == declaration.getBody() && descriptor is LazyClassDescriptor) {
            return descriptor.getDanglingAnnotations()
        }

        return descriptor.getAnnotations()
    }

    private fun typeParameterAdditionalResolve(analyzer: KotlinCodeAnalyzer, typeParameter: JetTypeParameter) {
        val descriptor = analyzer.resolveToDescriptor(typeParameter)
        ForceResolveUtil.forceResolveAllContents(descriptor)
    }

    private fun delegationSpecifierAdditionalResolve(resolveSession: ResolveSession, classOrObject: JetClassOrObject, trace: BindingTrace, file: JetFile) {
        val descriptor = resolveSession.resolveToDescriptor(classOrObject) as LazyClassDescriptor

        // Activate resolving of supertypes
        ForceResolveUtil.forceResolveAllContents(descriptor.getTypeConstructor().getSupertypes())

        val bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE)
        bodyResolver.resolveDelegationSpecifierList(createEmptyContext(),
                                                    classOrObject,
                                                    descriptor,
                                                    descriptor.getUnsubstitutedPrimaryConstructor(),
                                                    descriptor.getScopeForClassHeaderResolution(),
                                                    descriptor.getScopeForMemberDeclarationResolution())
    }

    private fun propertyAdditionalResolve(resolveSession: ResolveSession, jetProperty: JetProperty, trace: BindingTrace, file: JetFile, statementFilter: StatementFilter) {
        val propertyResolutionScope = resolveSession.getScopeProvider().getResolutionScopeForDeclaration(jetProperty)

        val bodyResolveContext = BodyResolveContextForLazy(TopDownAnalysisMode.LocalDeclarations, object : Function<JetDeclaration, JetScope> {
            override fun apply(declaration: JetDeclaration?): JetScope? {
                assert(declaration!!.getParent() == jetProperty) { "Must be called only for property accessors, but called for " + declaration }
                return resolveSession.getScopeProvider().getResolutionScopeForDeclaration(declaration)
            }
        })
        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        val descriptor = resolveSession.resolveToDescriptor(jetProperty) as PropertyDescriptor
        ForceResolveUtil.forceResolveAllContents(descriptor)

        val propertyInitializer = jetProperty.getInitializer()
        if (propertyInitializer != null) {
            bodyResolver.resolvePropertyInitializer(bodyResolveContext, jetProperty, descriptor, propertyInitializer, propertyResolutionScope)
        }

        val propertyDelegate = jetProperty.getDelegateExpression()
        if (propertyDelegate != null) {
            bodyResolver.resolvePropertyDelegate(bodyResolveContext, jetProperty, descriptor, propertyDelegate, propertyResolutionScope, propertyResolutionScope)
        }

        bodyResolver.resolvePropertyAccessors(bodyResolveContext, jetProperty, descriptor)

        for (accessor in jetProperty.getAccessors()) {
            JetFlowInformationProvider(accessor, trace).checkDeclaration()
        }
    }

    private fun functionAdditionalResolve(resolveSession: ResolveSession, namedFunction: JetNamedFunction, trace: BindingTrace, file: JetFile, statementFilter: StatementFilter) {
        val scope = resolveSession.getScopeProvider().getResolutionScopeForDeclaration(namedFunction)
        val functionDescriptor = resolveSession.resolveToDescriptor(namedFunction) as FunctionDescriptor
        ForceResolveUtil.forceResolveAllContents(functionDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveFunctionBody(createEmptyContext(), trace, namedFunction, functionDescriptor, scope)
    }

    private fun secondaryConstructorAdditionalResolve(resolveSession: ResolveSession, constructor: JetSecondaryConstructor, trace: BindingTrace, file: JetFile, statementFilter: StatementFilter) {
        val scope = resolveSession.getScopeProvider().getResolutionScopeForDeclaration(constructor)
        val constructorDescriptor = resolveSession.resolveToDescriptor(constructor) as ConstructorDescriptor
        ForceResolveUtil.forceResolveAllContents(constructorDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveSecondaryConstructorBody(createEmptyContext(), trace, constructor, constructorDescriptor, scope)
    }

    private fun constructorAdditionalResolve(resolveSession: ResolveSession, klass: JetClass, trace: BindingTrace, file: JetFile, statementFilter: StatementFilter) {
        val scope = resolveSession.getScopeProvider().getResolutionScopeForDeclaration(klass)

        val classDescriptor = resolveSession.resolveToDescriptor(klass) as ClassDescriptor
        val constructorDescriptor = classDescriptor.getUnsubstitutedPrimaryConstructor()
                                    ?: error("Can't get primary constructor for descriptor '$classDescriptor' in from class '${klass.getElementTextWithContext()}'")

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveConstructorParameterDefaultValuesAndAnnotations(createEmptyContext(), trace, klass, constructorDescriptor, scope)
    }

    private fun initializerAdditionalResolve(resolveSession: ResolveSession, classInitializer: JetClassInitializer, trace: BindingTrace, file: JetFile, statementFilter: StatementFilter) {
        val classOrObject = classInitializer.getParentOfType<JetClassOrObject>(true)!!
        val classOrObjectDescriptor = resolveSession.resolveToDescriptor(classOrObject) as LazyClassDescriptor

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveAnonymousInitializer(createEmptyContext(), classInitializer, classOrObjectDescriptor)
    }

    private fun createBodyResolver(resolveSession: ResolveSession, trace: BindingTrace, file: JetFile, statementFilter: StatementFilter): BodyResolver {
        val globalContext = SimpleGlobalContext(resolveSession.getStorageManager(), resolveSession.getExceptionTracker())
        val bodyResolve = InjectorForBodyResolve(
                globalContext.withProject(file.getProject()).withModule(resolveSession.getModuleDescriptor()),
                trace, getAdditionalCheckerProvider(file), statementFilter
        )
        return bodyResolve.getBodyResolver()
    }

    private fun createEmptyContext(): BodyResolveContextForLazy {
        return BodyResolveContextForLazy(TopDownAnalysisMode.LocalDeclarations, Functions.constant<JetScope>(null))
    }

    private fun getExpressionResolutionScope(resolveSession: ResolveSession, expression: JetExpression): JetScope {
        val provider = resolveSession.getScopeProvider()
        val parentDeclaration = expression.getParentOfType<JetDeclaration>(true)
        if (parentDeclaration == null) {
            return provider.getFileScope(expression.getContainingJetFile())
        }
        return provider.getResolutionScopeForDeclaration(parentDeclaration)
    }

    private fun getExpressionMemberScope(resolveSession: ResolveSession, expression: JetExpression): JetScope? {
        val trace = resolveSession.getStorageManager().createSafeTrace(
                DelegatingBindingTrace(resolveSession.getBindingContext(), "trace to resolve a member scope of expression", expression))

        if (BindingContextUtils.isExpressionWithValidReference(expression, resolveSession.getBindingContext())) {
            val qualifiedExpressionResolver = resolveSession.getQualifiedExpressionResolver()

            // In some type declaration
            val parent = expression.getParent()
            if (parent is JetUserType) {
                val qualifier = parent.getQualifier()
                if (qualifier != null) {
                    val resolutionScope = getExpressionResolutionScope(resolveSession, expression)
                    val descriptors = qualifiedExpressionResolver.lookupDescriptorsForUserType(qualifier, resolutionScope, trace, false)
                    return descriptors.firstIsInstanceOrNull<LazyPackageDescriptor>()?.getMemberScope()
                }
            }

            // Inside import
            if (expression.getParentOfType<JetImportDirective>(false) != null) {
                val rootPackage = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT)!!

                if (parent is JetDotQualifiedExpression) {
                    val element = parent.getReceiverExpression()
                    val fqName = expression.getContainingJetFile().getPackageFqName()

                    val filePackage = resolveSession.getModuleDescriptor().getPackage(fqName)
                                      ?: error("File package should be already resolved and be found")

                    val scope = filePackage.getMemberScope()
                    val descriptors = if (element is JetDotQualifiedExpression) {
                        qualifiedExpressionResolver.lookupDescriptorsForQualifiedExpression(
                                element, rootPackage.getMemberScope(), scope, trace, QualifiedExpressionResolver.LookupMode.EVERYTHING, false)
                    }
                    else {
                        qualifiedExpressionResolver.lookupDescriptorsForSimpleNameReference(
                                element as JetSimpleNameExpression, rootPackage.getMemberScope(), scope, trace, QualifiedExpressionResolver.LookupMode.EVERYTHING, false, false)
                    }

                    return descriptors.firstIsInstanceOrNull<PackageViewDescriptor>()?.getMemberScope()
                }
                else {
                    return rootPackage.getMemberScope()
                }
            }

            // Inside package declaration
            val packageDirective = expression.getParentOfType<JetPackageDirective>(false)
            if (packageDirective != null) {
                val packageDescriptor = resolveSession.getModuleDescriptor().getPackage(packageDirective.getFqName(expression as JetSimpleNameExpression).parent())
                return packageDescriptor?.getMemberScope()
            }
        }

        return null
    }

    protected abstract fun getAdditionalCheckerProvider(jetFile: JetFile): AdditionalCheckerProvider

    private class BodyResolveContextForLazy(
            private val topDownAnalysisMode: TopDownAnalysisMode,
            private val declaringScopes: Function<in JetDeclaration, JetScope>
    ) : BodiesResolveContext {

        override fun getFiles(): Collection<JetFile> = setOf()

        override fun getDeclaredClasses(): Map<JetClassOrObject, ClassDescriptorWithResolutionScopes> = mapOf()

        override fun getAnonymousInitializers(): Map<JetClassInitializer, ClassDescriptorWithResolutionScopes> = mapOf()

        override fun getSecondaryConstructors(): Map<JetSecondaryConstructor, ConstructorDescriptor> = mapOf()

        override fun getProperties(): Map<JetProperty, PropertyDescriptor> = mapOf()

        override fun getFunctions(): Map<JetNamedFunction, SimpleFunctionDescriptor> = mapOf()

        override fun getDeclaringScopes() = declaringScopes as Function<JetDeclaration, JetScope>

        override fun getScripts(): Map<JetScript, ScriptDescriptor> = mapOf()

        override fun getOuterDataFlowInfo() = DataFlowInfo.EMPTY

        override fun getTopDownAnalysisMode() = topDownAnalysisMode
    }
}
