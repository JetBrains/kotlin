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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.computeTypeInContext
import org.jetbrains.kotlin.cfg.JetFlowInformationProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.frontend.di.createContainerForBodyResolve
import org.jetbrains.kotlin.resolve.util.getScopeAndDataFlowForAnalyzeFragment
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
import org.jetbrains.kotlin.types.DynamicTypesSettings
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public abstract class ElementResolver protected constructor(
        public val resolveSession: ResolveSession
) {
    public open fun getElementAdditionalResolve(resolveElement: JetElement, contextElement: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
        return performElementAdditionalResolve(resolveElement, resolveElement, bodyResolveMode).first
    }

    protected open fun probablyNothingCallableNames(): ProbablyNothingCallableNames
            = throw UnsupportedOperationException("Cannot use partial body resolve with no Nothing-functions index");

    public fun resolveToElement(element: JetElement, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext {
        var contextElement = element

        val elementOfAdditionalResolve = findElementOfAdditionalResolve(contextElement)

        if (elementOfAdditionalResolve is JetParameter) {
            contextElement = elementOfAdditionalResolve
        }
        else if (elementOfAdditionalResolve != null) {
            return getElementAdditionalResolve(elementOfAdditionalResolve, contextElement, bodyResolveMode)
        }

        val declaration = contextElement.getParentOfType<JetDeclaration>(false)
        if (declaration != null && declaration !is JetClassInitializer) {
            // Activate descriptor resolution
            resolveSession.resolveToDescriptor(declaration)
        }

        return resolveSession.getBindingContext()
    }

    private fun findElementOfAdditionalResolve(element: JetElement): JetElement? {
        val elementOfAdditionalResolve = JetPsiUtil.getTopmostParentOfTypes(
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
                javaClass<JetCodeFragment>()) as JetElement? ?: return null

        when (elementOfAdditionalResolve) {
            is JetPackageDirective -> return element

            is JetParameter -> {
                val klass = elementOfAdditionalResolve.getParentOfType<JetClass>(strict = true)
                if (klass != null && elementOfAdditionalResolve.getParent() == klass.getPrimaryConstructorParameterList()) {
                    return klass
                }

                return elementOfAdditionalResolve
            }

            else -> return elementOfAdditionalResolve
        }
    }

    protected fun performElementAdditionalResolve(resolveElement: JetElement, contextElement: JetElement, bodyResolveMode: BodyResolveMode): Pair<BindingContext, StatementFilter> {
        val file = resolveElement.getContainingJetFile()

        val statementFilter = if (bodyResolveMode != BodyResolveMode.FULL && resolveElement is JetDeclaration)
            PartialBodyResolveFilter(contextElement, resolveElement, probablyNothingCallableNames(), bodyResolveMode == BodyResolveMode.PARTIAL_FOR_COMPLETION)
        else
            StatementFilter.NONE

        val trace : BindingTrace = when (resolveElement) {
            is JetNamedFunction -> functionAdditionalResolve(resolveSession, resolveElement, file, statementFilter)

            is JetClassInitializer -> initializerAdditionalResolve(resolveSession, resolveElement, file, statementFilter)

            is JetSecondaryConstructor -> secondaryConstructorAdditionalResolve(resolveSession, resolveElement, file, statementFilter)

            is JetProperty -> propertyAdditionalResolve(resolveSession, resolveElement, file, statementFilter)

            is JetDelegationSpecifierList -> delegationSpecifierAdditionalResolve(resolveSession, resolveElement, resolveElement.getParent() as JetClassOrObject, file)

            is JetInitializerList -> delegationSpecifierAdditionalResolve(resolveSession, resolveElement, resolveElement.getParent() as JetEnumEntry, file)

            is JetImportDirective -> {
                val scope = resolveSession.getFileScopeProvider().getFileScope(resolveElement.getContainingJetFile())
                scope.forceResolveAllImports()
                resolveSession.getTrace()
            }

            is JetAnnotationEntry -> annotationAdditionalResolve(resolveSession, resolveElement)

            is JetClass -> constructorAdditionalResolve(resolveSession, resolveElement, file, statementFilter)

            is JetTypeParameter -> typeParameterAdditionalResolve(resolveSession, resolveElement)

            is JetTypeConstraint -> typeConstraintAdditionalResolve(resolveSession, resolveElement)

            is JetCodeFragment -> codeFragmentAdditionalResolve(resolveSession, resolveElement, bodyResolveMode)

            else -> {
                if (resolveElement.getParentOfType<JetPackageDirective>(true) != null) {
                    packageRefAdditionalResolve(resolveSession, resolveElement)
                }
                else {
                    error("Invalid type of the topmost parent: $resolveElement\n${resolveElement.getElementTextWithContext()}")
                }
            }
        }

        val controlFlowTrace = DelegatingBindingTrace(trace.getBindingContext(), "Element control flow resolve", resolveElement)
        JetFlowInformationProvider(resolveElement, controlFlowTrace).checkDeclaration()
        controlFlowTrace.addOwnDataTo(trace, null, false)

        return Pair(trace.getBindingContext(), statementFilter)
    }

    private fun packageRefAdditionalResolve(resolveSession: ResolveSession, jetElement: JetElement): BindingTrace {
        val trace = createDelegatingTrace(jetElement)

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
                    trace.record(BindingContext.REFERENCE_TARGET, jetElement, packageDescriptor)
                }
            }
        }

        return trace
    }

    private fun typeConstraintAdditionalResolve(analyzer: KotlinCodeAnalyzer, jetTypeConstraint: JetTypeConstraint): BindingTrace {
        val declaration = jetTypeConstraint.getParentOfType<JetDeclaration>(true)!!
        val descriptor = analyzer.resolveToDescriptor(declaration) as ClassDescriptor

        for (parameterDescriptor in descriptor.getTypeConstructor().getParameters()) {
            ForceResolveUtil.forceResolveAllContents<TypeParameterDescriptor>(parameterDescriptor)
        }

        return resolveSession.getTrace()
    }

    private fun codeFragmentAdditionalResolve(resolveSession: ResolveSession, codeFragment: JetCodeFragment, bodyResolveMode: BodyResolveMode): BindingTrace {
        val trace = createDelegatingTrace(codeFragment)

        val codeFragmentExpression = codeFragment.getContentElement() as? JetExpression ?: return trace

        val (scopeForContextElement, dataFlowInfoForContextElement) = codeFragment.getScopeAndDataFlowForAnalyzeFragment(resolveSession) {
            val contextResolveMode = if (bodyResolveMode == BodyResolveMode.PARTIAL)
                BodyResolveMode.PARTIAL_FOR_COMPLETION
            else
                bodyResolveMode

            resolveToElement(it, contextResolveMode)
        } ?: return trace

        codeFragmentExpression.computeTypeInContext(scopeForContextElement, trace, dataFlowInfoForContextElement,
                                                    TypeUtils.NO_EXPECTED_TYPE, resolveSession.getModuleDescriptor())

        return trace
    }

    private fun annotationAdditionalResolve(resolveSession: ResolveSession, jetAnnotationEntry: JetAnnotationEntry): BindingTrace {
        val modifierList = jetAnnotationEntry.getParentOfType<JetModifierList>(true)
        val declaration = modifierList?.getParentOfType<JetDeclaration>(true)
        if (declaration != null) {
            doResolveAnnotations(getAnnotationsByDeclaration(resolveSession, modifierList!!, declaration))
        }
        else {
            val fileAnnotationList = jetAnnotationEntry.getParentOfType<JetFileAnnotationList>(true)
            if (fileAnnotationList != null) {
                doResolveAnnotations(resolveSession.getFileAnnotations(fileAnnotationList.getContainingJetFile()))
            }
            if (modifierList != null && modifierList.getParent() is JetFile) {
                doResolveAnnotations(resolveSession.getDanglingAnnotations(modifierList.getContainingJetFile()))
            }
        }

        return resolveSession.getTrace()
    }

    private fun doResolveAnnotations(annotations: Annotations) {
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

    private fun typeParameterAdditionalResolve(analyzer: KotlinCodeAnalyzer, typeParameter: JetTypeParameter): BindingTrace {
        val descriptor = analyzer.resolveToDescriptor(typeParameter)
        ForceResolveUtil.forceResolveAllContents(descriptor)

        return resolveSession.getTrace()
    }

    private fun delegationSpecifierAdditionalResolve(resolveSession: ResolveSession, jetElement: JetElement, classOrObject: JetClassOrObject, file: JetFile): BindingTrace {
        val trace = createDelegatingTrace(jetElement)
        val descriptor = resolveSession.resolveToDescriptor(classOrObject) as LazyClassDescriptor

        // Activate resolving of supertypes
        ForceResolveUtil.forceResolveAllContents(descriptor.getTypeConstructor().getSupertypes())

        val bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE)
        bodyResolver.resolveDelegationSpecifierList(DataFlowInfo.EMPTY,
                                                    classOrObject,
                                                    descriptor,
                                                    descriptor.getUnsubstitutedPrimaryConstructor(),
                                                    descriptor.getScopeForClassHeaderResolution(),
                                                    descriptor.getScopeForMemberDeclarationResolution())

        return trace
    }

    private fun propertyAdditionalResolve(resolveSession: ResolveSession, jetProperty: JetProperty, file: JetFile, statementFilter: StatementFilter): BindingTrace {
        val trace = createDelegatingTrace(jetProperty)
        val propertyResolutionScope = resolveSession.getDeclarationScopeProvider().getResolutionScopeForDeclaration(jetProperty)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        val descriptor = resolveSession.resolveToDescriptor(jetProperty) as PropertyDescriptor
        ForceResolveUtil.forceResolveAllContents(descriptor)

        val propertyInitializer = jetProperty.getInitializer()
        if (propertyInitializer != null) {
            bodyResolver.resolvePropertyInitializer(DataFlowInfo.EMPTY, jetProperty, descriptor, propertyInitializer, propertyResolutionScope)
        }

        val propertyDelegate = jetProperty.getDelegateExpression()
        if (propertyDelegate != null) {
            bodyResolver.resolvePropertyDelegate(DataFlowInfo.EMPTY, jetProperty, descriptor, propertyDelegate, propertyResolutionScope, propertyResolutionScope)
        }

        val bodyResolveContext = BodyResolveContextForLazy(TopDownAnalysisMode.LocalDeclarations, { declaration ->
            assert(declaration.getParent() == jetProperty) { "Must be called only for property accessors, but called for $declaration" }
            resolveSession.getDeclarationScopeProvider().getResolutionScopeForDeclaration(declaration)
        })

        bodyResolver.resolvePropertyAccessors(bodyResolveContext, jetProperty, descriptor)

        for (accessor in jetProperty.getAccessors()) {
            JetFlowInformationProvider(accessor, trace).checkDeclaration()
        }

        return trace
    }

    private fun functionAdditionalResolve(resolveSession: ResolveSession, namedFunction: JetNamedFunction, file: JetFile, statementFilter: StatementFilter): BindingTrace {
        val trace = createDelegatingTrace(namedFunction)

        val scope = resolveSession.getDeclarationScopeProvider().getResolutionScopeForDeclaration(namedFunction)
        val functionDescriptor = resolveSession.resolveToDescriptor(namedFunction) as FunctionDescriptor
        ForceResolveUtil.forceResolveAllContents(functionDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveFunctionBody(DataFlowInfo.EMPTY, trace, namedFunction, functionDescriptor, scope)

        return trace
    }

    private fun secondaryConstructorAdditionalResolve(resolveSession: ResolveSession, constructor: JetSecondaryConstructor, file: JetFile, statementFilter: StatementFilter): BindingTrace {
        val trace = createDelegatingTrace(constructor)

        val scope = resolveSession.getDeclarationScopeProvider().getResolutionScopeForDeclaration(constructor)
        val constructorDescriptor = resolveSession.resolveToDescriptor(constructor) as ConstructorDescriptor
        ForceResolveUtil.forceResolveAllContents(constructorDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveSecondaryConstructorBody(DataFlowInfo.EMPTY, trace, constructor, constructorDescriptor, scope)

        return trace
    }

    private fun constructorAdditionalResolve(resolveSession: ResolveSession, klass: JetClass, file: JetFile, statementFilter: StatementFilter): BindingTrace {
        val trace = createDelegatingTrace(klass)
        val scope = resolveSession.getDeclarationScopeProvider().getResolutionScopeForDeclaration(klass)

        val classDescriptor = resolveSession.resolveToDescriptor(klass) as ClassDescriptor
        val constructorDescriptor = classDescriptor.getUnsubstitutedPrimaryConstructor()
                                    ?: error("Can't get primary constructor for descriptor '$classDescriptor' in from class '${klass.getElementTextWithContext()}'")

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveConstructorParameterDefaultValuesAndAnnotations(DataFlowInfo.EMPTY, trace, klass, constructorDescriptor, scope)

        return trace
    }

    private fun initializerAdditionalResolve(resolveSession: ResolveSession, classInitializer: JetClassInitializer, file: JetFile, statementFilter: StatementFilter): BindingTrace {
        val trace = createDelegatingTrace(classInitializer)

        val classOrObject = classInitializer.getParentOfType<JetClassOrObject>(true)!!
        val classOrObjectDescriptor = resolveSession.resolveToDescriptor(classOrObject) as LazyClassDescriptor

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveAnonymousInitializer(DataFlowInfo.EMPTY, classInitializer, classOrObjectDescriptor)

        return trace
    }

    private fun createBodyResolver(
            resolveSession: ResolveSession,
            trace: BindingTrace,
            file: JetFile,
            statementFilter: StatementFilter
    ): BodyResolver {
        val globalContext = SimpleGlobalContext(resolveSession.getStorageManager(), resolveSession.getExceptionTracker())
        val module = resolveSession.getModuleDescriptor()
        return createContainerForBodyResolve(
                globalContext.withProject(file.getProject()).withModule(module),
                trace, createAdditionalCheckerProvider(file, module), statementFilter, getDynamicTypesSettings(file)
        ).get<BodyResolver>()
    }

    // All additional resolve should be done to separate trace
    private fun createDelegatingTrace(resolveElement: JetElement): BindingTrace {
        return resolveSession.getStorageManager().createSafeTrace(
                DelegatingBindingTrace(resolveSession.getBindingContext(), "trace to resolve element", resolveElement))
    }

    private fun getExpressionResolutionScope(resolveSession: ResolveSession, expression: JetExpression): JetScope {
        val parentDeclaration = expression.getParentOfType<JetDeclaration>(true)
        if (parentDeclaration == null) {
            return resolveSession.getFileScopeProvider().getFileScope(expression.getContainingJetFile())
        }
        return resolveSession.getDeclarationScopeProvider().getResolutionScopeForDeclaration(parentDeclaration)
    }

    private fun getExpressionMemberScope(resolveSession: ResolveSession, expression: JetExpression): JetScope? {
        val trace = createDelegatingTrace(expression)

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
                val rootPackage = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT)

                if (parent is JetDotQualifiedExpression) {
                    val element = parent.getReceiverExpression()
                    val fqName = expression.getContainingJetFile().getPackageFqName()

                    val filePackage = resolveSession.getModuleDescriptor().getPackage(fqName)

                    val descriptors = if (element is JetDotQualifiedExpression) {
                        qualifiedExpressionResolver.lookupDescriptorsForQualifiedExpression(
                                element, rootPackage.memberScope, filePackage, trace, QualifiedExpressionResolver.LookupMode.EVERYTHING, false)
                    }
                    else {
                        qualifiedExpressionResolver.lookupDescriptorsForSimpleNameReference(
                                element as JetSimpleNameExpression, rootPackage.memberScope, filePackage, trace, QualifiedExpressionResolver.LookupMode.EVERYTHING, false, false)
                    }

                    return descriptors.firstIsInstanceOrNull<PackageViewDescriptor>()?.memberScope
                }
                else {
                    return rootPackage.memberScope
                }
            }

            // Inside package declaration
            val packageDirective = expression.getParentOfType<JetPackageDirective>(false)
            if (packageDirective != null) {
                val packageDescriptor = resolveSession.getModuleDescriptor().getPackage(packageDirective.getFqName(expression as JetSimpleNameExpression).parent())
                return packageDescriptor.memberScope
            }
        }

        return null
    }

    protected abstract fun createAdditionalCheckerProvider(file: JetFile, module: ModuleDescriptor): AdditionalCheckerProvider

    protected abstract fun getDynamicTypesSettings(file: JetFile): DynamicTypesSettings

    private class BodyResolveContextForLazy(
            private val topDownAnalysisMode: TopDownAnalysisMode,
            private val declaringScopes: Function1<JetDeclaration, JetScope?>
    ) : BodiesResolveContext {
        override fun getFiles(): Collection<JetFile> = setOf()

        override fun getDeclaredClasses(): MutableMap<JetClassOrObject, ClassDescriptorWithResolutionScopes> = hashMapOf()

        override fun getAnonymousInitializers(): MutableMap<JetClassInitializer, ClassDescriptorWithResolutionScopes> = hashMapOf()

        override fun getSecondaryConstructors(): MutableMap<JetSecondaryConstructor, ConstructorDescriptor> = hashMapOf()

        override fun getProperties(): MutableMap<JetProperty, PropertyDescriptor> = hashMapOf()

        override fun getFunctions(): MutableMap<JetNamedFunction, SimpleFunctionDescriptor> = hashMapOf()

        override fun getDeclaringScope(declaration: JetDeclaration): JetScope? = declaringScopes(declaration)

        override fun getScripts(): MutableMap<JetScript, ScriptDescriptor> = hashMapOf()

        override fun getOuterDataFlowInfo(): DataFlowInfo = DataFlowInfo.EMPTY

        override fun getTopDownAnalysisMode() = topDownAnalysisMode
    }
}

