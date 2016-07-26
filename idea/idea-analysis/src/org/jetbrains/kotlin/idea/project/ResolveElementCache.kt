/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.cfg.ControlFlowInformationProvider
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.frontend.di.createContainerForBodyResolve
import org.jetbrains.kotlin.idea.caches.resolve.CodeFragmentAnalyzer
import org.jetbrains.kotlin.idea.stubindex.KotlinProbablyNothingFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinProbablyNothingPropertyShortNameIndex
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyScriptDescriptor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.utils.addToStdlib.check

class ResolveElementCache(
        private val resolveSession: ResolveSession,
        private val project: Project,
        private val targetPlatform: TargetPlatform,
        private val codeFragmentAnalyzer: CodeFragmentAnalyzer
) : BodyResolveCache {
    private class CachedFullResolve(val bindingContext: BindingContext, resolveElement: KtElement) {
        private val modificationStamp: Long? = modificationStamp(resolveElement)

        fun isUpToDate(resolveElement: KtElement) = modificationStamp == modificationStamp(resolveElement)

        private fun modificationStamp(resolveElement: KtElement): Long? {
            val file = resolveElement.containingFile
            return if (!file.isPhysical) // for non-physical file we don't get OUT_OF_CODE_BLOCK_MODIFICATION_COUNT increased and must reset data on any modification of the file
                file.modificationStamp
            else if (resolveElement is KtDeclaration && KotlinCodeBlockModificationListener.isBlockDeclaration(resolveElement))
                resolveElement.getModificationStamp()
            else
                null
        }
    }

    // drop whole cache after change "out of code block"
    private val fullResolveCache: CachedValue<MutableMap<KtElement, CachedFullResolve>> = CachedValuesManager.getManager(project).createCachedValue(
            object : CachedValueProvider<MutableMap<KtElement, CachedFullResolve>> {
                override fun compute(): CachedValueProvider.Result<MutableMap<KtElement, CachedFullResolve>> {
                    return CachedValueProvider.Result.create(ContainerUtil.createConcurrentSoftValueMap<KtElement, CachedFullResolve>(),
                                                             PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
                                                             resolveSession.exceptionTracker)
                }
            },
            false)

    private class CachedPartialResolve(val bindingContext: BindingContext, file: KtFile) {
        private val modificationStamp: Long? = modificationStamp(file)

        fun isUpToDate(file: KtFile) = modificationStamp == modificationStamp(file)

        private fun modificationStamp(file: KtFile): Long? {
            return if (!file.isPhysical) // for non-physical file we don't get MODIFICATION_COUNT increased and must reset data on any modification of the file
                file.modificationStamp
            else
                null
        }
    }

    private val partialBodyResolveCache: CachedValue<MutableMap<KtExpression, CachedPartialResolve>> = CachedValuesManager.getManager(project).createCachedValue(
            object : CachedValueProvider<MutableMap<KtExpression, CachedPartialResolve>> {
                override fun compute(): CachedValueProvider.Result<MutableMap<KtExpression, CachedPartialResolve>> {
                    return CachedValueProvider.Result.create(ContainerUtil.createConcurrentSoftValueMap<KtExpression, CachedPartialResolve>(),
                                                             PsiModificationTracker.MODIFICATION_COUNT,
                                                             resolveSession.exceptionTracker)
                }
            },
            false)


    private fun probablyNothingCallableNames(): ProbablyNothingCallableNames {
        return object : ProbablyNothingCallableNames {
            override fun functionNames() = KotlinProbablyNothingFunctionShortNameIndex.getInstance().getAllKeys(project)
            override fun propertyNames() = KotlinProbablyNothingPropertyShortNameIndex.getInstance().getAllKeys(project)
        }
    }

    override fun resolveFunctionBody(function: KtNamedFunction)
            = getElementAdditionalResolve(function, function, BodyResolveMode.FULL)

    fun resolvePrimaryConstructorParametersDefaultValues(ktClass: KtClass): BindingContext {
        return constructorAdditionalResolve(resolveSession, ktClass, ktClass.getContainingKtFile()).bindingContext
    }

    fun getElementAdditionalResolve(resolveElement: KtElement, contextElement: KtElement, bodyResolveMode: BodyResolveMode): BindingContext {
        // check if full additional resolve already performed and is up-to-date
        val fullResolveMap = fullResolveCache.value
        val cachedFullResolve = fullResolveMap[resolveElement]
        if (cachedFullResolve != null) {
            if (cachedFullResolve.isUpToDate(resolveElement)) {
                return cachedFullResolve.bindingContext
            }
            else {
                fullResolveMap.remove(resolveElement) // remove outdated cache entry
            }
        }

        when (bodyResolveMode) {
            BodyResolveMode.FULL -> {
                val bindingContext = performElementAdditionalResolve(resolveElement, resolveElement, BodyResolveMode.FULL).first
                fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                return bindingContext
            }

            BodyResolveMode.PARTIAL -> {
                if (resolveElement !is KtDeclaration) {
                    return getElementAdditionalResolve(resolveElement, contextElement, BodyResolveMode.FULL)
                }

                val file = resolveElement.getContainingKtFile()
                val statementToResolve = PartialBodyResolveFilter.findStatementToResolve(contextElement, resolveElement)
                val partialResolveMap = partialBodyResolveCache.value
                partialResolveMap[statementToResolve ?: resolveElement]
                        ?.check { it.isUpToDate(file) }
                        ?.let { return it.bindingContext } // partial resolve is already cached for this statement
                val (bindingContext, statementFilter) = performElementAdditionalResolve(resolveElement, contextElement, BodyResolveMode.PARTIAL)

                if (statementFilter == StatementFilter.NONE) { // partial resolve is not supported for the given declaration - full resolve performed instead
                    fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                    return bindingContext
                }

                val resolveToCache = CachedPartialResolve(bindingContext, file)

                for (statement in (statementFilter as PartialBodyResolveFilter).allStatementsToResolve) {
                    if (!partialResolveMap.containsKey(statement) && bindingContext[BindingContext.PROCESSED, statement] == true) {
                        partialResolveMap[statement] = resolveToCache
                    }
                }
                partialResolveMap[resolveElement] = resolveToCache // we use the whole declaration key in the map to obtain resolve not inside any block (e.g. default parameter values)

                return bindingContext
            }

            BodyResolveMode.PARTIAL_FOR_COMPLETION -> {
                if (resolveElement !is KtDeclaration) {
                    return getElementAdditionalResolve(resolveElement, contextElement, BodyResolveMode.FULL)
                }

                // not cached
                return performElementAdditionalResolve(resolveElement, contextElement, bodyResolveMode).first
            }
        }
    }

    fun resolveToElement(element: KtElement, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext {
        var contextElement = element

        val elementOfAdditionalResolve = findElementOfAdditionalResolve(contextElement)

        if (elementOfAdditionalResolve is KtParameter) {
            // Parameters for function literal could be met inside other parameters. We can't make resolveToDescriptors for internal elements.
            contextElement = elementOfAdditionalResolve
        }
        else if (elementOfAdditionalResolve != null) {
            return getElementAdditionalResolve(elementOfAdditionalResolve, contextElement, bodyResolveMode)
        }

        val declaration = contextElement.getParentOfType<KtDeclaration>(false)
        if (declaration != null && declaration !is KtAnonymousInitializer) {
            // Activate descriptor resolution
            resolveSession.resolveToDescriptor(declaration)
        }

        return resolveSession.bindingContext
    }

    private fun findElementOfAdditionalResolve(element: KtElement): KtElement? {
        val elementOfAdditionalResolve = KtPsiUtil.getTopmostParentOfTypes(
                element,
                KtNamedFunction::class.java,
                KtAnonymousInitializer::class.java,
                KtSecondaryConstructor::class.java,
                KtProperty::class.java,
                KtParameter::class.java,
                KtSuperTypeList::class.java,
                KtInitializerList::class.java,
                KtImportList::class.java,
                KtAnnotationEntry::class.java,
                KtTypeParameter::class.java,
                KtTypeConstraint::class.java,
                KtPackageDirective::class.java,
                KtCodeFragment::class.java,
                KtTypeAlias::class.java) as KtElement?

        when (elementOfAdditionalResolve) {
            null -> {
                // Case of JetAnnotationEntry on top level class
                if (element is KtAnnotationEntry) {
                    return element
                }

                if (element is KtFileAnnotationList) {
                    return element
                }

                return null
            }

            is KtPackageDirective -> return element

            is KtParameter -> {
                val klass = elementOfAdditionalResolve.getParentOfType<KtClass>(strict = true)
                if (klass != null && elementOfAdditionalResolve.getParent() == klass.getPrimaryConstructorParameterList()) {
                    return klass
                }

                return elementOfAdditionalResolve
            }

            is KtDeclaration -> {
                if (element is KtParameter && !KtPsiUtil.isLocal(element)) {
                    return null
                }

                return elementOfAdditionalResolve
            }

            else -> return elementOfAdditionalResolve
        }
    }

    private fun performElementAdditionalResolve(resolveElement: KtElement, contextElement: KtElement, bodyResolveMode: BodyResolveMode): Pair<BindingContext, StatementFilter> {
        val file = resolveElement.getContainingKtFile()

        var statementFilterUsed = StatementFilter.NONE

        fun createStatementFilter(): StatementFilter {
            assert(resolveElement is KtDeclaration)
            if (bodyResolveMode != BodyResolveMode.FULL) {
                statementFilterUsed = PartialBodyResolveFilter(
                        contextElement,
                        resolveElement as KtDeclaration,
                        probablyNothingCallableNames(),
                        bodyResolveMode == BodyResolveMode.PARTIAL_FOR_COMPLETION)
            }
            return statementFilterUsed
        }

        val trace: BindingTrace = when (resolveElement) {
            is KtNamedFunction -> functionAdditionalResolve(resolveSession, resolveElement, file, createStatementFilter())

            is KtAnonymousInitializer -> initializerAdditionalResolve(resolveSession, resolveElement, file, createStatementFilter())

            is KtSecondaryConstructor -> secondaryConstructorAdditionalResolve(resolveSession, resolveElement, file, createStatementFilter())

            is KtProperty -> propertyAdditionalResolve(resolveSession, resolveElement, file, createStatementFilter())

            is KtSuperTypeList -> delegationSpecifierAdditionalResolve(resolveSession, resolveElement, resolveElement.getParent() as KtClassOrObject, file)

            is KtInitializerList -> delegationSpecifierAdditionalResolve(resolveSession, resolveElement, resolveElement.getParent() as KtEnumEntry, file)

            is KtImportList -> {
                val resolver = resolveSession.fileScopeProvider.getImportResolver(resolveElement.getContainingKtFile())
                resolver.forceResolveAllImports()
                resolveSession.trace
            }

            is KtFileAnnotationList -> {
                val annotationEntry = resolveElement.annotationEntries.firstOrNull()
                if (annotationEntry != null) {
                    annotationAdditionalResolve(resolveSession, annotationEntry)
                }
                else {
                    resolveSession.trace
                }
            }

            is KtAnnotationEntry -> annotationAdditionalResolve(resolveSession, resolveElement)

            is KtClass -> constructorAdditionalResolve(resolveSession, resolveElement, file)

            is KtTypeAlias -> typealiasAdditionalResolve(resolveSession, resolveElement)

            is KtTypeParameter -> typeParameterAdditionalResolve(resolveSession, resolveElement)

            is KtTypeConstraint -> typeConstraintAdditionalResolve(resolveSession, resolveElement)

            is KtCodeFragment -> codeFragmentAdditionalResolve(resolveSession, resolveElement, bodyResolveMode)

            else -> {
                if (resolveElement.getParentOfType<KtPackageDirective>(true) != null) {
                    packageRefAdditionalResolve(resolveSession, resolveElement)
                }
                else {
                    error("Invalid type of the topmost parent: $resolveElement\n${resolveElement.getElementTextWithContext()}")
                }
            }
        }

        val controlFlowTrace = DelegatingBindingTrace(trace.bindingContext, "Element control flow resolve", resolveElement)
        ControlFlowInformationProvider(resolveElement, controlFlowTrace).checkDeclaration()
        controlFlowTrace.addOwnDataTo(trace, null, false)

        return Pair(trace.bindingContext, statementFilterUsed)
    }

    private fun packageRefAdditionalResolve(resolveSession: ResolveSession, ktElement: KtElement): BindingTrace {
        val trace = createDelegatingTrace(ktElement)

        if (ktElement is KtSimpleNameExpression) {
            val header = ktElement.getParentOfType<KtPackageDirective>(true)!!

            if (Name.isValidIdentifier(ktElement.getReferencedName())) {
                if (trace.bindingContext[BindingContext.REFERENCE_TARGET, ktElement] == null) {
                    val fqName = header.getFqName(ktElement)
                    val packageDescriptor = resolveSession.moduleDescriptor.getPackage(fqName)
                    trace.record(BindingContext.REFERENCE_TARGET, ktElement, packageDescriptor)
                }
            }
        }

        return trace
    }

    private fun typeConstraintAdditionalResolve(analyzer: KotlinCodeAnalyzer, jetTypeConstraint: KtTypeConstraint): BindingTrace {
        val declaration = jetTypeConstraint.getParentOfType<KtDeclaration>(true)!!
        val descriptor = analyzer.resolveToDescriptor(declaration) as ClassifierDescriptorWithTypeParameters

        for (parameterDescriptor in descriptor.declaredTypeParameters) {
            ForceResolveUtil.forceResolveAllContents<TypeParameterDescriptor>(parameterDescriptor)
        }

        return resolveSession.trace
    }

    private fun codeFragmentAdditionalResolve(resolveSession: ResolveSession, codeFragment: KtCodeFragment, bodyResolveMode: BodyResolveMode): BindingTrace {
        val trace = createDelegatingTrace(codeFragment)

        val contextResolveMode = if (bodyResolveMode == BodyResolveMode.PARTIAL)
            BodyResolveMode.PARTIAL_FOR_COMPLETION
        else
            bodyResolveMode
        codeFragmentAnalyzer.analyzeCodeFragment(codeFragment, trace, contextResolveMode)

        return trace
    }

    private fun annotationAdditionalResolve(resolveSession: ResolveSession, ktAnnotationEntry: KtAnnotationEntry): BindingTrace {
        val modifierList = ktAnnotationEntry.getParentOfType<KtModifierList>(true)
        val declaration = modifierList?.getParentOfType<KtDeclaration>(true)
        if (declaration != null) {
            doResolveAnnotations(getAnnotationsByDeclaration(resolveSession, modifierList!!, declaration))
        }
        else {
            val fileAnnotationList = ktAnnotationEntry.getParentOfType<KtFileAnnotationList>(true)
            if (fileAnnotationList != null) {
                doResolveAnnotations(resolveSession.getFileAnnotations(fileAnnotationList.getContainingKtFile()))
            }
            if (modifierList != null && modifierList.parent is KtFile) {
                doResolveAnnotations(resolveSession.getDanglingAnnotations(modifierList.getContainingKtFile()))
            }
        }

        return resolveSession.trace
    }

    private fun doResolveAnnotations(annotations: Annotations) {
        ForceResolveUtil.forceResolveAllContents(annotations)
    }

    private fun getAnnotationsByDeclaration(resolveSession: ResolveSession, modifierList: KtModifierList, declaration: KtDeclaration): Annotations {
        var descriptor = resolveSession.resolveToDescriptor(declaration)
        if (declaration is KtClass) {
            if (modifierList == declaration.getPrimaryConstructorModifierList()) {
                descriptor = (descriptor as ClassDescriptor).unsubstitutedPrimaryConstructor
                             ?: error("No constructor found: ${declaration.getText()}")
            }
        }

        if (declaration is KtClassOrObject && modifierList.parent == declaration.getBody() && descriptor is LazyClassDescriptor) {
            return descriptor.danglingAnnotations
        }

        return descriptor.annotations
    }

    private fun typeParameterAdditionalResolve(analyzer: KotlinCodeAnalyzer, typeParameter: KtTypeParameter): BindingTrace {
        val descriptor = analyzer.resolveToDescriptor(typeParameter)
        ForceResolveUtil.forceResolveAllContents(descriptor)

        return resolveSession.trace
    }

    private fun delegationSpecifierAdditionalResolve(resolveSession: ResolveSession, ktElement: KtElement, classOrObject: KtClassOrObject, file: KtFile): BindingTrace {
        val trace = createDelegatingTrace(ktElement)
        val descriptor = resolveSession.resolveToDescriptor(classOrObject) as LazyClassDescriptor

        // Activate resolving of supertypes
        ForceResolveUtil.forceResolveAllContents(descriptor.typeConstructor.supertypes)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE)
        bodyResolver.resolveSuperTypeEntryList(DataFlowInfo.EMPTY,
                                               classOrObject,
                                               descriptor,
                                               descriptor.unsubstitutedPrimaryConstructor,
                                               descriptor.scopeForConstructorHeaderResolution,
                                               descriptor.scopeForMemberDeclarationResolution)

        return trace
    }

    private fun propertyAdditionalResolve(resolveSession: ResolveSession, property: KtProperty, file: KtFile, statementFilter: StatementFilter): BindingTrace {
        val trace = createDelegatingTrace(property)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        val descriptor = resolveSession.resolveToDescriptor(property) as PropertyDescriptor
        ForceResolveUtil.forceResolveAllContents(descriptor)

        val bodyResolveContext = BodyResolveContextForLazy(TopDownAnalysisMode.LocalDeclarations, { declaration ->
            assert(declaration.parent == property || declaration == property) {
                "Must be called only for property accessors or for property, but called for $declaration"
            }
            resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(declaration)
        })

        bodyResolver.resolveProperty(bodyResolveContext, property, descriptor)

        forceResolveAnnotationsInside(property)

        for (accessor in property.accessors) {
            ControlFlowInformationProvider(accessor, trace).checkDeclaration()
        }

        return trace
    }

    private fun functionAdditionalResolve(resolveSession: ResolveSession, namedFunction: KtNamedFunction, file: KtFile, statementFilter: StatementFilter): BindingTrace {
        val trace = createDelegatingTrace(namedFunction)

        val scope = resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(namedFunction)
        val functionDescriptor = resolveSession.resolveToDescriptor(namedFunction) as FunctionDescriptor
        ForceResolveUtil.forceResolveAllContents(functionDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveFunctionBody(DataFlowInfo.EMPTY, trace, namedFunction, functionDescriptor, scope)

        forceResolveAnnotationsInside(namedFunction)

        return trace
    }

    private fun secondaryConstructorAdditionalResolve(resolveSession: ResolveSession, constructor: KtSecondaryConstructor, file: KtFile, statementFilter: StatementFilter): BindingTrace {
        val trace = createDelegatingTrace(constructor)

        val scope = resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(constructor)
        val constructorDescriptor = resolveSession.resolveToDescriptor(constructor) as ConstructorDescriptor
        ForceResolveUtil.forceResolveAllContents(constructorDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveSecondaryConstructorBody(DataFlowInfo.EMPTY, trace, constructor, constructorDescriptor, scope)

        forceResolveAnnotationsInside(constructor)

        return trace
    }

    private fun constructorAdditionalResolve(resolveSession: ResolveSession, klass: KtClass, file: KtFile): BindingTrace {
        val trace = createDelegatingTrace(klass)
        val scope = resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(klass)

        val classDescriptor = resolveSession.resolveToDescriptor(klass) as ClassDescriptor
        val constructorDescriptor = classDescriptor.unsubstitutedPrimaryConstructor
                                    ?: error("Can't get primary constructor for descriptor '$classDescriptor' in from class '${klass.getElementTextWithContext()}'")

        val bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE)
        bodyResolver.resolveConstructorParameterDefaultValuesAndAnnotations(DataFlowInfo.EMPTY, trace, klass, constructorDescriptor, scope)

        return trace
    }

    private fun typealiasAdditionalResolve(resolveSession: ResolveSession, typeAlias: KtTypeAlias): BindingTrace {
        val trace = createDelegatingTrace(typeAlias)
        val typeAliasDescriptor = resolveSession.resolveToDescriptor(typeAlias)
        ForceResolveUtil.forceResolveAllContents(typeAliasDescriptor)
        forceResolveAnnotationsInside(typeAlias)
        return trace
    }

    private fun initializerAdditionalResolve(resolveSession: ResolveSession, anonymousInitializer: KtAnonymousInitializer, file: KtFile, statementFilter: StatementFilter): BindingTrace {
        val trace = createDelegatingTrace(anonymousInitializer)

        val classOrObjectDescriptor = resolveSession.resolveToDescriptor(anonymousInitializer.containingDeclaration) as LazyClassDescriptor

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveAnonymousInitializer(DataFlowInfo.EMPTY, anonymousInitializer, classOrObjectDescriptor)

        forceResolveAnnotationsInside(anonymousInitializer)

        return trace
    }

    private fun forceResolveAnnotationsInside(element: KtElement) {
        element.forEachDescendantOfType<KtAnnotationEntry>(canGoInside = { it !is KtBlockExpression }) { entry ->
            resolveSession.bindingContext[BindingContext.ANNOTATION, entry]?.let {
                ForceResolveUtil.forceResolveAllContents(it)
            }
        }
    }

    private fun createBodyResolver(
            resolveSession: ResolveSession,
            trace: BindingTrace,
            file: KtFile,
            statementFilter: StatementFilter
    ): BodyResolver {
        val globalContext = SimpleGlobalContext(resolveSession.storageManager, resolveSession.exceptionTracker)
        val module = resolveSession.moduleDescriptor
        return createContainerForBodyResolve(
                globalContext.withProject(file.project).withModule(module),
                trace,
                targetPlatform,
                statementFilter,
                LanguageVersion.LATEST // TODO: see KT-12410
        ).get<BodyResolver>()
    }

    // All additional resolve should be done to separate trace
    private fun createDelegatingTrace(resolveElement: KtElement): BindingTrace {
        return resolveSession.storageManager.createSafeTrace(
                DelegatingBindingTrace(resolveSession.bindingContext, "trace to resolve element", resolveElement))
    }

    private class BodyResolveContextForLazy(
            private val topDownAnalysisMode: TopDownAnalysisMode,
            private val declaringScopes: Function1<KtDeclaration, LexicalScope?>
    ) : BodiesResolveContext {
        override fun getFiles(): Collection<KtFile> = setOf()

        override fun getDeclaredClasses(): MutableMap<KtClassOrObject, ClassDescriptorWithResolutionScopes> = hashMapOf()

        override fun getAnonymousInitializers(): MutableMap<KtAnonymousInitializer, ClassDescriptorWithResolutionScopes> = hashMapOf()

        override fun getSecondaryConstructors(): MutableMap<KtSecondaryConstructor, ConstructorDescriptor> = hashMapOf()

        override fun getProperties(): MutableMap<KtProperty, PropertyDescriptor> = hashMapOf()

        override fun getFunctions(): MutableMap<KtNamedFunction, SimpleFunctionDescriptor> = hashMapOf()

        override fun getTypeAliases(): MutableMap<KtTypeAlias, TypeAliasDescriptor> = hashMapOf()

        override fun getDeclaringScope(declaration: KtDeclaration): LexicalScope? = declaringScopes(declaration)

        override fun getScripts(): MutableMap<KtScript, LazyScriptDescriptor> = hashMapOf()

        override fun getOuterDataFlowInfo(): DataFlowInfo = DataFlowInfo.EMPTY

        override fun getTopDownAnalysisMode() = topDownAnalysisMode
    }
}

