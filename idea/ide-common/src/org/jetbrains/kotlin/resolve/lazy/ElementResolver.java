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
import com.intellij.psi.util.PsiTreeUtil
import kotlin.KotlinPackage
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.cfg.JetFlowInformationProvider
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.di.InjectorForBodyResolve
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.storage.ExceptionTracker
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeUtils
import java.util.Collections

import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage.getDataFlowInfo

public abstract class ElementResolver protected(protected val resolveSession: ResolveSession) {

    public open fun getElementAdditionalResolve(jetElement: JetElement): BindingContext {
        return elementAdditionalResolve(jetElement, jetElement, BodyResolveMode.FULL)
    }

    public open fun hasElementAdditionalResolveCached(jetElement: JetElement): Boolean {
        return false
    }

    public fun resolveToElement(jetElement: JetElement): BindingContext {
        return resolveToElement(jetElement, BodyResolveMode.FULL)
    }

    protected open fun probablyNothingCallableNames(): ProbablyNothingCallableNames {
        return DefaultNothingCallableNames
    }

    public fun resolveToElement(jetElement: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
        var jetElement = jetElement
        [SuppressWarnings("unchecked")] var elementOfAdditionalResolve: JetElement? = JetPsiUtil.getTopmostParentOfTypes(jetElement, javaClass<JetNamedFunction>(), javaClass<JetClassInitializer>(), javaClass<JetSecondaryConstructor>(), javaClass<JetProperty>(), javaClass<JetParameter>(), javaClass<JetDelegationSpecifierList>(), javaClass<JetInitializerList>(), javaClass<JetImportDirective>(), javaClass<JetAnnotationEntry>(), javaClass<JetTypeParameter>(), javaClass<JetTypeConstraint>(), javaClass<JetPackageDirective>(), javaClass<JetCodeFragment>()) as JetElement

        if (elementOfAdditionalResolve != null && !(elementOfAdditionalResolve is JetParameter)) {
            if (elementOfAdditionalResolve is JetPackageDirective) {
                elementOfAdditionalResolve = jetElement
            }

            if (bodyResolveMode != BodyResolveMode.FULL && !hasElementAdditionalResolveCached(jetElement)) {
                return elementAdditionalResolve(elementOfAdditionalResolve, jetElement, bodyResolveMode)
            }

            return getElementAdditionalResolve(elementOfAdditionalResolve)
        }

        val parameter = elementOfAdditionalResolve as JetParameter
        if (parameter != null) {
            val klass = PsiTreeUtil.getParentOfType<JetClass>(parameter, javaClass<JetClass>())
            if (klass != null && parameter.getParent() == klass.getPrimaryConstructorParameterList()) {
                return getElementAdditionalResolve(klass)
            }

            // Parameters for function literal could be met inside other parameters. We can't make resolveToDescriptors for internal elements.
            jetElement = parameter
        }

        val declaration = PsiTreeUtil.getParentOfType<JetDeclaration>(jetElement, javaClass<JetDeclaration>(), false)
        if (declaration != null && !(declaration is JetClassInitializer)) {
            // Activate descriptor resolution
            resolveSession.resolveToDescriptor(declaration)
        }

        return resolveSession.getBindingContext()
    }

    protected fun elementAdditionalResolve(resolveElement: JetElement, contextElement: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
        // All additional resolve should be done to separate trace
        val trace = resolveSession.getStorageManager().createSafeTrace(DelegatingBindingTrace(resolveSession.getBindingContext(), "trace to resolve element", resolveElement))

        val file = resolveElement.getContainingJetFile()

        val statementFilter: StatementFilter
        if (bodyResolveMode != BodyResolveMode.FULL && resolveElement is JetDeclaration) {
            statementFilter = PartialBodyResolveFilter(contextElement, resolveElement as JetDeclaration, probablyNothingCallableNames(), bodyResolveMode == BodyResolveMode.PARTIAL_FOR_COMPLETION)
        }
        else {
            statementFilter = StatementFilter.NONE
        }

        if (resolveElement is JetNamedFunction) {
            functionAdditionalResolve(resolveSession, resolveElement as JetNamedFunction, trace, file, statementFilter)
        }
        else if (resolveElement is JetClassInitializer) {
            initializerAdditionalResolve(resolveSession, resolveElement as JetClassInitializer, trace, file, statementFilter)
        }
        else if (resolveElement is JetSecondaryConstructor) {
            secondaryConstructorAdditionalResolve(resolveSession, resolveElement as JetSecondaryConstructor, trace, file, statementFilter)
        }
        else if (resolveElement is JetProperty) {
            propertyAdditionalResolve(resolveSession, resolveElement as JetProperty, trace, file, statementFilter)
        }
        else if (resolveElement is JetDelegationSpecifierList) {
            delegationSpecifierAdditionalResolve(resolveSession, resolveElement.getParent() as JetClassOrObject, trace, file)
        }
        else if (resolveElement is JetInitializerList) {
            delegationSpecifierAdditionalResolve(resolveSession, resolveElement.getParent() as JetEnumEntry, trace, file)
        }
        else if (resolveElement is JetImportDirective) {
            val importDirective = resolveElement as JetImportDirective
            val scope = resolveSession.getScopeProvider().getFileScope(importDirective.getContainingJetFile())
            scope.forceResolveAllImports()
        }
        else if (resolveElement is JetAnnotationEntry) {
            annotationAdditionalResolve(resolveSession, resolveElement as JetAnnotationEntry)
        }
        else if (resolveElement is JetClass) {
            constructorAdditionalResolve(resolveSession, resolveElement as JetClass, trace, file, statementFilter)
        }
        else if (resolveElement is JetTypeParameter) {
            typeParameterAdditionalResolve(resolveSession, resolveElement as JetTypeParameter)
        }
        else if (resolveElement is JetTypeConstraint) {
            typeConstraintAdditionalResolve(resolveSession, resolveElement as JetTypeConstraint)
        }
        else if (resolveElement is JetCodeFragment) {
            codeFragmentAdditionalResolve(resolveSession, resolveElement as JetCodeFragment, trace, bodyResolveMode)
        }
        else if (PsiTreeUtil.getParentOfType<JetPackageDirective>(resolveElement, javaClass<JetPackageDirective>()) != null) {
            packageRefAdditionalResolve(resolveSession, trace, resolveElement)
        }
        else {
            assert(false) { String.format("Invalid type of the topmost parent: %s\n%s", resolveElement.toString(), JetPsiUtil.getElementTextWithContext(resolveElement)) }
        }

        JetFlowInformationProvider(resolveElement, trace).checkDeclaration()

        return trace.getBindingContext()
    }

    private fun packageRefAdditionalResolve(resolveSession: ResolveSession, trace: BindingTrace, jetElement: JetElement) {
        if (jetElement is JetSimpleNameExpression) {
            val header = PsiTreeUtil.getParentOfType<JetPackageDirective>(jetElement, javaClass<JetPackageDirective>())
            assert(header != null)

            val packageNameExpression = jetElement as JetSimpleNameExpression
            if (trace.getBindingContext().get<JetExpression, JetScope>(BindingContext.RESOLUTION_SCOPE, packageNameExpression) == null) {
                val scope = getExpressionMemberScope(resolveSession, packageNameExpression)
                if (scope != null) {
                    trace.record<JetExpression, JetScope>(BindingContext.RESOLUTION_SCOPE, packageNameExpression, scope)
                }
            }

            if (Name.isValidIdentifier(packageNameExpression.getReferencedName())) {
                if (trace.getBindingContext().get<JetReferenceExpression, DeclarationDescriptor>(BindingContext.REFERENCE_TARGET, packageNameExpression) == null) {
                    val fqName = header!!.getFqName(packageNameExpression)
                    val packageDescriptor = resolveSession.getModuleDescriptor().getPackage(fqName)
                    assert(packageDescriptor != null) { "Package descriptor should be present in session for " + fqName }
                    trace.record<JetReferenceExpression, DeclarationDescriptor>(BindingContext.REFERENCE_TARGET, packageNameExpression, packageDescriptor)
                }
            }
        }
    }

    private fun typeConstraintAdditionalResolve(analyzer: KotlinCodeAnalyzer, jetTypeConstraint: JetTypeConstraint) {
        val declaration = PsiTreeUtil.getParentOfType<JetDeclaration>(jetTypeConstraint, javaClass<JetDeclaration>())
        val descriptor = analyzer.resolveToDescriptor(declaration)

        assert((descriptor is ClassDescriptor))

        val constructor = (descriptor as ClassDescriptor).getTypeConstructor()
        for (parameterDescriptor in constructor.getParameters()) {
            ForceResolveUtil.forceResolveAllContents<TypeParameterDescriptor>(parameterDescriptor)
        }
    }

    private fun codeFragmentAdditionalResolve(resolveSession: ResolveSession, codeFragment: JetCodeFragment, trace: BindingTrace, bodyResolveMode: BodyResolveMode) {
        val codeFragmentExpression = codeFragment.getContentElement()
        if (!(codeFragmentExpression is JetExpression)) return

        val contextElement = codeFragment.getContext()

        val scopeForContextElement: JetScope?
        val dataFlowInfoForContextElement: DataFlowInfo

        if (contextElement is JetClassOrObject) {
            val descriptor = resolveSession.resolveToDescriptor(contextElement as JetClassOrObject) as LazyClassDescriptor

            scopeForContextElement = descriptor.getScopeForMemberDeclarationResolution()
            dataFlowInfoForContextElement = DataFlowInfo.EMPTY
        }
        else if (contextElement is JetBlockExpression) {
            val newContextElement = KotlinPackage.lastOrNull<JetElement>((contextElement as JetBlockExpression).getStatements())

            if (!(newContextElement is JetExpression)) return

            val contextForElement = resolveToElement(contextElement as JetElement, BodyResolveMode.FULL)

            scopeForContextElement = contextForElement.get<JetExpression, JetScope>(BindingContext.RESOLUTION_SCOPE, (newContextElement as JetExpression))
            dataFlowInfoForContextElement = contextForElement.getDataFlowInfo(newContextElement as JetExpression)
        }
        else {
            if (!(contextElement is JetExpression)) return

            val contextExpression = contextElement as JetExpression
            val contextForElement = resolveToElement(contextElement as JetElement, bodyResolveMode)

            scopeForContextElement = contextForElement.get<JetExpression, JetScope>(BindingContext.RESOLUTION_SCOPE, contextExpression)
            dataFlowInfoForContextElement = contextForElement.getDataFlowInfo(contextExpression)
        }

        if (scopeForContextElement == null) return

        val codeFragmentScope = resolveSession.getScopeProvider().getFileScope(codeFragment)
        val chainedScope = ChainedScope(scopeForContextElement.getContainingDeclaration(), "Scope for resolve code fragment", scopeForContextElement, codeFragmentScope)

        (codeFragmentExpression as JetExpression).computeTypeInContext(chainedScope, trace, dataFlowInfoForContextElement, TypeUtils.NO_EXPECTED_TYPE, resolveSession.getModuleDescriptor())
    }

    private fun annotationAdditionalResolve(resolveSession: ResolveSession, jetAnnotationEntry: JetAnnotationEntry) {
        val modifierList = PsiTreeUtil.getParentOfType<JetModifierList>(jetAnnotationEntry, javaClass<JetModifierList>())
        val declaration = PsiTreeUtil.getParentOfType<JetDeclaration>(modifierList, javaClass<JetDeclaration>())
        if (declaration != null) {
            doResolveAnnotations(resolveSession, getAnnotationsByDeclaration(resolveSession, modifierList, declaration))
        }
        else {
            val fileAnnotationList = PsiTreeUtil.getParentOfType<JetFileAnnotationList>(jetAnnotationEntry, javaClass<JetFileAnnotationList>())
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
        var descriptor: Annotated? = resolveSession.resolveToDescriptor(declaration)
        if (declaration is JetClass) {
            val jetClass = declaration as JetClass
            val classDescriptor = descriptor as ClassDescriptor
            if (modifierList == jetClass.getPrimaryConstructorModifierList()) {
                descriptor = classDescriptor.getUnsubstitutedPrimaryConstructor()
                assert(descriptor != null) { "No constructor found: " + declaration.getText() }
            }
            else if (modifierList.getParent() == jetClass.getBody()) {
                if (classDescriptor is LazyClassDescriptor) {
                    return (classDescriptor as LazyClassDescriptor).getDanglingAnnotations()
                }
            }
        }
        return descriptor!!.getAnnotations()
    }

    private fun typeParameterAdditionalResolve(analyzer: KotlinCodeAnalyzer, typeParameter: JetTypeParameter) {
        val descriptor = analyzer.resolveToDescriptor(typeParameter)
        ForceResolveUtil.forceResolveAllContents<DeclarationDescriptor>(descriptor)
    }

    private fun delegationSpecifierAdditionalResolve(resolveSession: ResolveSession, classOrObject: JetClassOrObject, trace: BindingTrace, file: JetFile) {
        val descriptor = resolveSession.resolveToDescriptor(classOrObject) as LazyClassDescriptor

        // Activate resolving of supertypes
        ForceResolveUtil.forceResolveAllContents(descriptor.getTypeConstructor().getSupertypes())

        val bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE)
        bodyResolver.resolveDelegationSpecifierList(createEmptyContext(resolveSession), classOrObject, descriptor, descriptor.getUnsubstitutedPrimaryConstructor(), descriptor.getScopeForClassHeaderResolution(), descriptor.getScopeForMemberDeclarationResolution())
    }

    private fun propertyAdditionalResolve(resolveSession: ResolveSession, jetProperty: JetProperty, trace: BindingTrace, file: JetFile, statementFilter: StatementFilter) {
        val propertyResolutionScope = resolveSession.getScopeProvider().getResolutionScopeForDeclaration(jetProperty)

        val bodyResolveContext = BodyResolveContextForLazy(createParameters(resolveSession), object : Function<JetDeclaration, JetScope> {
            override fun apply(declaration: JetDeclaration?): JetScope? {
                assert(declaration!!.getParent() == jetProperty) { "Must be called only for property accessors, but called for " + declaration }
                return resolveSession.getScopeProvider().getResolutionScopeForDeclaration(declaration)
            }
        })
        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        val descriptor = resolveSession.resolveToDescriptor(jetProperty) as PropertyDescriptor
        ForceResolveUtil.forceResolveAllContents<PropertyDescriptor>(descriptor)

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
        ForceResolveUtil.forceResolveAllContents<FunctionDescriptor>(functionDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveFunctionBody(createEmptyContext(resolveSession), trace, namedFunction, functionDescriptor, scope)
    }

    private fun secondaryConstructorAdditionalResolve(resolveSession: ResolveSession, constructor: JetSecondaryConstructor, trace: BindingTrace, file: JetFile, statementFilter: StatementFilter) {
        val scope = resolveSession.getScopeProvider().getResolutionScopeForDeclaration(constructor)
        val constructorDescriptor = resolveSession.resolveToDescriptor(constructor) as ConstructorDescriptor
        ForceResolveUtil.forceResolveAllContents<ConstructorDescriptor>(constructorDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveSecondaryConstructorBody(createEmptyContext(resolveSession), trace, constructor, constructorDescriptor, scope)
    }

    private fun constructorAdditionalResolve(resolveSession: ResolveSession, klass: JetClass, trace: BindingTrace, file: JetFile, statementFilter: StatementFilter) {
        val scope = resolveSession.getScopeProvider().getResolutionScopeForDeclaration(klass)

        val classDescriptor = resolveSession.resolveToDescriptor(klass) as ClassDescriptor
        val constructorDescriptor = classDescriptor.getUnsubstitutedPrimaryConstructor()
        assert(constructorDescriptor != null) { String.format("Can't get primary constructor for descriptor '%s' in from class '%s'", classDescriptor, JetPsiUtil.getElementTextWithContext(klass)) }

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveConstructorParameterDefaultValuesAndAnnotations(createEmptyContext(resolveSession), trace, klass, constructorDescriptor, scope)
    }

    private fun initializerAdditionalResolve(resolveSession: ResolveSession, classInitializer: JetClassInitializer, trace: BindingTrace, file: JetFile, statementFilter: StatementFilter) {
        val classOrObject = PsiTreeUtil.getParentOfType<JetClassOrObject>(classInitializer, javaClass<JetClassOrObject>())
        val classOrObjectDescriptor = resolveSession.resolveToDescriptor(classOrObject) as LazyClassDescriptor

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveAnonymousInitializer(createEmptyContext(resolveSession), classInitializer, classOrObjectDescriptor)
    }

    private fun createBodyResolver(resolveSession: ResolveSession, trace: BindingTrace, file: JetFile, statementFilter: StatementFilter): BodyResolver {
        val bodyResolve = InjectorForBodyResolve(file.getProject(), createParameters(resolveSession), trace, resolveSession.getModuleDescriptor(), getAdditionalCheckerProvider(file), statementFilter)
        return bodyResolve.getBodyResolver()
    }

    private fun createParameters(resolveSession: ResolveSession): TopDownAnalysisParameters {
        return TopDownAnalysisParameters.createForLocalDeclarations(resolveSession.getStorageManager(), resolveSession.getExceptionTracker())
    }

    private fun createEmptyContext(resolveSession: ResolveSession): BodyResolveContextForLazy {
        return BodyResolveContextForLazy(createParameters(resolveSession), Functions.constant<JetScope>(null))
    }

    private fun getExpressionResolutionScope(resolveSession: ResolveSession, expression: JetExpression): JetScope {
        val provider = resolveSession.getScopeProvider()
        val parentDeclaration = PsiTreeUtil.getParentOfType<JetDeclaration>(expression, javaClass<JetDeclaration>())
        if (parentDeclaration == null) {
            return provider.getFileScope(expression.getContainingJetFile())
        }
        return provider.getResolutionScopeForDeclaration(parentDeclaration)
    }

    private fun getExpressionMemberScope(resolveSession: ResolveSession, expression: JetExpression): JetScope? {
        val trace = resolveSession.getStorageManager().createSafeTrace(DelegatingBindingTrace(resolveSession.getBindingContext(), "trace to resolve a member scope of expression", expression))

        if (BindingContextUtils.isExpressionWithValidReference(expression, resolveSession.getBindingContext())) {
            val qualifiedExpressionResolver = resolveSession.getQualifiedExpressionResolver()

            // In some type declaration
            if (expression.getParent() is JetUserType) {
                val qualifier = (expression.getParent() as JetUserType).getQualifier()
                if (qualifier != null) {
                    val resolutionScope = getExpressionResolutionScope(resolveSession, expression)
                    val descriptors = qualifiedExpressionResolver.lookupDescriptorsForUserType(qualifier, resolutionScope, trace, false)
                    for (descriptor in descriptors) {
                        if (descriptor is LazyPackageDescriptor) {
                            return (descriptor as LazyPackageDescriptor).getMemberScope()
                        }
                    }
                }
            }

            // Inside import
            if (PsiTreeUtil.getParentOfType<JetImportDirective>(expression, javaClass<JetImportDirective>(), false) != null) {
                val rootPackage = resolveSession.getModuleDescriptor().getPackage(FqName.ROOT)
                assert(rootPackage != null)

                if (expression.getParent() is JetDotQualifiedExpression) {
                    val element = (expression.getParent() as JetDotQualifiedExpression).getReceiverExpression()
                    val fqName = expression.getContainingJetFile().getPackageFqName()

                    val filePackage = resolveSession.getModuleDescriptor().getPackage(fqName)
                    assert(filePackage != null, "File package should be already resolved and be found")

                    val scope = filePackage!!.getMemberScope()
                    val descriptors: Collection<out DeclarationDescriptor>

                    if (element is JetDotQualifiedExpression) {
                        descriptors = qualifiedExpressionResolver.lookupDescriptorsForQualifiedExpression(element as JetDotQualifiedExpression, rootPackage!!.getMemberScope(), scope, trace, QualifiedExpressionResolver.LookupMode.EVERYTHING, false)
                    }
                    else {
                        descriptors = qualifiedExpressionResolver.lookupDescriptorsForSimpleNameReference(element as JetSimpleNameExpression, rootPackage!!.getMemberScope(), scope, trace, QualifiedExpressionResolver.LookupMode.EVERYTHING, false, false)
                    }

                    for (descriptor in descriptors) {
                        if (descriptor is PackageViewDescriptor) {
                            return (descriptor as PackageViewDescriptor).getMemberScope()
                        }
                    }
                }
                else {
                    return rootPackage!!.getMemberScope()
                }
            }

            // Inside package declaration
            val packageDirective = PsiTreeUtil.getParentOfType<JetPackageDirective>(expression, javaClass<JetPackageDirective>(), false)
            if (packageDirective != null) {
                val packageDescriptor = resolveSession.getModuleDescriptor().getPackage(packageDirective.getFqName(expression as JetSimpleNameExpression).parent())
                if (packageDescriptor != null) {
                    return packageDescriptor.getMemberScope()
                }
            }
        }

        return null
    }

    protected abstract fun getAdditionalCheckerProvider(jetFile: JetFile): AdditionalCheckerProvider

    private class BodyResolveContextForLazy private(private val topDownAnalysisParameters: TopDownAnalysisParameters, private val declaringScopes: Function<in JetDeclaration, JetScope>) : BodiesResolveContext {

        override fun getStorageManager(): StorageManager {
            return topDownAnalysisParameters.getStorageManager()
        }

        override fun getExceptionTracker(): ExceptionTracker {
            return topDownAnalysisParameters.getExceptionTracker()
        }

        override fun getFiles(): Collection<JetFile> {
            return setOf()
        }

        override fun getDeclaredClasses(): Map<JetClassOrObject, ClassDescriptorWithResolutionScopes> {
            return mapOf()
        }

        override fun getAnonymousInitializers(): Map<JetClassInitializer, ClassDescriptorWithResolutionScopes> {
            return mapOf()
        }

        override fun getSecondaryConstructors(): Map<JetSecondaryConstructor, ConstructorDescriptor> {
            return mapOf()
        }

        override fun getProperties(): Map<JetProperty, PropertyDescriptor> {
            return mapOf()
        }

        override fun getFunctions(): Map<JetNamedFunction, SimpleFunctionDescriptor> {
            return mapOf()
        }

        override fun getDeclaringScopes(): Function<JetDeclaration, JetScope> {
            //noinspection unchecked
            return declaringScopes as Function<JetDeclaration, JetScope>
        }

        override fun getScripts(): Map<JetScript, ScriptDescriptor> {
            return mapOf()
        }

        override fun getOuterDataFlowInfo(): DataFlowInfo {
            return DataFlowInfo.EMPTY
        }

        override fun getTopDownAnalysisParameters(): TopDownAnalysisParameters {
            return topDownAnalysisParameters
        }
    }
}
