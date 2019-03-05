/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.frontend.di.createContainerForBodyResolve
import org.jetbrains.kotlin.idea.caches.resolve.CodeFragmentAnalyzer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import java.util.*

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
            return when {
            // for non-physical file we don't get OUT_OF_CODE_BLOCK_MODIFICATION_COUNT increased and must reset
            // data on any modification of the file
                !file.isPhysical -> file.modificationStamp

                resolveElement is KtDeclaration && KotlinCodeBlockModificationListener.isBlockDeclaration(resolveElement) -> resolveElement.getModificationStamp()
                resolveElement is KtSuperTypeList -> resolveElement.modificationStamp
                else -> null
            }
        }
    }

    // drop whole cache after change "out of code block", each entry is checked with own modification stamp
    private val fullResolveCache: CachedValue<MutableMap<KtElement, CachedFullResolve>> =
        CachedValuesManager.getManager(project).createCachedValue(
            CachedValueProvider<MutableMap<KtElement, ResolveElementCache.CachedFullResolve>> {
                CachedValueProvider.Result.create(
                    ContainerUtil.createConcurrentWeakKeySoftValueMap<KtElement, CachedFullResolve>(),
                    PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
                    resolveSession.exceptionTracker
                )
            },
            false
        )

    private class CachedPartialResolve(val bindingContext: BindingContext, file: KtFile, val mode: BodyResolveMode) {
        private val modificationStamp: Long? = modificationStamp(file)

        fun isUpToDate(file: KtFile, newMode: BodyResolveMode) =
            modificationStamp == modificationStamp(file) && mode.doesNotLessThan(newMode)

        private fun modificationStamp(file: KtFile): Long? {
            return if (!file.isPhysical) // for non-physical file we don't get MODIFICATION_COUNT increased and must reset data on any modification of the file
                file.modificationStamp
            else
                null
        }
    }

    private val partialBodyResolveCache: CachedValue<MutableMap<KtExpression, CachedPartialResolve>> =
        CachedValuesManager.getManager(project).createCachedValue(
            CachedValueProvider<MutableMap<KtExpression, ResolveElementCache.CachedPartialResolve>> {
                CachedValueProvider.Result.create(
                    ContainerUtil.createConcurrentWeakKeySoftValueMap<KtExpression, CachedPartialResolve>(),
                    PsiModificationTracker.MODIFICATION_COUNT,
                    resolveSession.exceptionTracker
                )
            },
            false
        )

    override fun resolveFunctionBody(function: KtNamedFunction) = getElementsAdditionalResolve(function, null, BodyResolveMode.FULL)

    fun resolvePrimaryConstructorParametersDefaultValues(ktClass: KtClass): BindingContext {
        return constructorAdditionalResolve(
            resolveSession,
            ktClass,
            ktClass.containingKtFile,
            BindingTraceFilter.NO_DIAGNOSTICS
        ).bindingContext
    }

    @Deprecated("Use getElementsAdditionalResolve")
    fun getElementAdditionalResolve(
        resolveElement: KtElement,
        contextElement: KtElement,
        bodyResolveMode: BodyResolveMode
    ): BindingContext {
        return getElementsAdditionalResolve(resolveElement, listOf(contextElement), bodyResolveMode)
    }

    fun getElementsAdditionalResolve(
        resolveElement: KtElement,
        contextElements: Collection<KtElement>?,
        bodyResolveMode: BodyResolveMode
    ): BindingContext {
        if (contextElements == null) {
            assert(bodyResolveMode == BodyResolveMode.FULL)
        }

        // check if full additional resolve already performed and is up-to-date
        val fullResolveMap = fullResolveCache.value
        val cachedFullResolve = fullResolveMap[resolveElement]
        if (cachedFullResolve != null) {
            if (cachedFullResolve.isUpToDate(resolveElement)) {
                return cachedFullResolve.bindingContext
            } else {
                fullResolveMap.remove(resolveElement) // remove outdated cache entry
            }
        }

        when (bodyResolveMode) {
            BodyResolveMode.FULL -> {
                val bindingContext = performElementAdditionalResolve(resolveElement, null, BodyResolveMode.FULL).first
                fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                return bindingContext
            }

            else -> {
                if (resolveElement !is KtDeclaration) {
                    return getElementsAdditionalResolve(resolveElement, null, BodyResolveMode.FULL)
                }

                val file = resolveElement.getContainingKtFile()
                val statementsToResolve =
                    contextElements!!.map { PartialBodyResolveFilter.findStatementToResolve(it, resolveElement) }.distinct()
                val partialResolveMap = partialBodyResolveCache.value
                val cachedResults = statementsToResolve.map { partialResolveMap[it ?: resolveElement] }
                if (cachedResults.all {
                        it != null && it.isUpToDate(
                            file,
                            bodyResolveMode
                        )
                    }) { // partial resolve is already cached for these statements
                    return CompositeBindingContext.create(cachedResults.map { it!!.bindingContext }.distinct())
                }

                val (bindingContext, statementFilter) = performElementAdditionalResolve(resolveElement, contextElements, bodyResolveMode)

                if (statementFilter == StatementFilter.NONE &&
                    bodyResolveMode.doControlFlowAnalysis && !bodyResolveMode.bindingTraceFilter.ignoreDiagnostics
                ) {
                    // Without statement filter, we analyze everything, so we can count partial resolve result as full resolve
                    // But we can do this only if our resolve mode also provides *both* CFA and diagnostics
                    // This is true only for PARTIAL_WITH_DIAGNOSTICS resolve mode
                    fullResolveMap[resolveElement] = CachedFullResolve(bindingContext, resolveElement)
                    return bindingContext
                }

                val resolveToCache = CachedPartialResolve(bindingContext, file, bodyResolveMode)

                if (statementFilter is PartialBodyResolveFilter) {
                    for (statement in statementFilter.allStatementsToResolve) {
                        if (bindingContext[BindingContext.PROCESSED, statement] == true) {
                            partialResolveMap.putIfAbsent(statement, resolveToCache)
                        }
                    }
                }

                // we use the whole declaration key in the map to obtain resolve not inside any block (e.g. default parameter values)
                partialResolveMap[resolveElement] = resolveToCache

                return bindingContext
            }
        }
    }

    fun resolveToElements(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext {
        val elementsByAdditionalResolveElement: Map<KtElement?, List<KtElement>> = elements.groupBy { findElementOfAdditionalResolve(it) }

        val bindingContexts = ArrayList<BindingContext>()
        val declarationsToResolve = ArrayList<KtDeclaration>()
        var addResolveSessionBindingContext = false
        for ((elementOfAdditionalResolve, contextElements) in elementsByAdditionalResolveElement) {
            if (elementOfAdditionalResolve != null) {
                if (elementOfAdditionalResolve is KtParameter) {
                    throw AssertionError(
                        "ResolveElementCache: Element of additional resolve should not be KtParameter: " +
                                "${elementOfAdditionalResolve.text} for context element ${contextElements.firstOrNull()?.text}"
                    )
                }
                val bindingContext = getElementsAdditionalResolve(elementOfAdditionalResolve, contextElements, bodyResolveMode)
                bindingContexts.add(bindingContext)
            } else {
                contextElements
                    .mapNotNull { it.getNonStrictParentOfType<KtDeclaration>() }
                    .filterTo(declarationsToResolve) {
                        it !is KtAnonymousInitializer && it !is KtDestructuringDeclaration && it !is KtDestructuringDeclarationEntry
                    }
                addResolveSessionBindingContext = true
            }
        }

        declarationsToResolve.forEach { resolveSession.resolveToDescriptor(it) }
        if (addResolveSessionBindingContext) {
            bindingContexts.add(resolveSession.bindingContext)
        }

        //TODO: it can be slow if too many contexts
        return CompositeBindingContext.create(bindingContexts)
    }

    private fun findElementOfAdditionalResolve(element: KtElement): KtElement? {
        val elementOfAdditionalResolve = KtPsiUtil.getTopmostParentOfTypes(
            element,
            KtNamedFunction::class.java,
            KtAnonymousInitializer::class.java,
            KtPrimaryConstructor::class.java,
            KtSecondaryConstructor::class.java,
            KtProperty::class.java,
            KtSuperTypeList::class.java,
            KtInitializerList::class.java,
            KtImportList::class.java,
            KtAnnotationEntry::class.java,
            KtTypeParameter::class.java,
            KtTypeConstraint::class.java,
            KtPackageDirective::class.java,
            KtCodeFragment::class.java,
            KtTypeAlias::class.java,
            KtDestructuringDeclaration::class.java
        ) as KtElement?

        when (elementOfAdditionalResolve) {
            null -> {
                // Case of JetAnnotationEntry on top level class
                if (element is KtAnnotationEntry) {
                    return element
                }

                if (element is KtFileAnnotationList) {
                    return element
                }

                // Case of pure script element, like val (x, y) = ... on top of the script
                return element.getParentOfType<KtScript>(strict = false)
            }

            is KtPackageDirective -> return element

            is KtDeclaration -> {
                if (element is KtParameter && !KtPsiUtil.isLocal(element)) {
                    return null
                }

                return elementOfAdditionalResolve
            }

            else -> return elementOfAdditionalResolve
        }
    }

    private fun performElementAdditionalResolve(
        resolveElement: KtElement,
        contextElements: Collection<KtElement>?,
        bodyResolveMode: BodyResolveMode
    ): Pair<BindingContext, StatementFilter> {
        if (contextElements == null) {
            assert(bodyResolveMode == BodyResolveMode.FULL)
        }

        val file = resolveElement.containingKtFile

        var statementFilterUsed = StatementFilter.NONE

        fun createStatementFilter(): StatementFilter {
            assert(resolveElement is KtDeclaration)
            if (bodyResolveMode != BodyResolveMode.FULL) {
                statementFilterUsed = PartialBodyResolveFilter(
                    contextElements!!,
                    resolveElement as KtDeclaration,
                    bodyResolveMode == BodyResolveMode.PARTIAL_FOR_COMPLETION
                )
            }
            return statementFilterUsed
        }

        val trace: BindingTrace = when (resolveElement) {
            is KtDestructuringDeclaration -> destructuringDeclarationAdditionalResolve(
                resolveSession,
                resolveElement,
                bodyResolveMode.bindingTraceFilter
            )

            is KtNamedFunction -> functionAdditionalResolve(
                resolveSession,
                resolveElement,
                file,
                createStatementFilter(),
                bodyResolveMode.bindingTraceFilter
            )

            is KtAnonymousInitializer -> initializerAdditionalResolve(
                resolveSession,
                resolveElement,
                file,
                createStatementFilter(),
                bodyResolveMode.bindingTraceFilter
            )

            is KtPrimaryConstructor -> constructorAdditionalResolve(
                resolveSession,
                resolveElement.parent as KtClassOrObject,
                file,
                bodyResolveMode.bindingTraceFilter
            )

            is KtSecondaryConstructor -> secondaryConstructorAdditionalResolve(
                resolveSession,
                resolveElement,
                file,
                createStatementFilter(),
                bodyResolveMode.bindingTraceFilter
            )

            is KtProperty -> propertyAdditionalResolve(
                resolveSession,
                resolveElement,
                file,
                createStatementFilter(),
                bodyResolveMode.bindingTraceFilter
            )

            is KtSuperTypeList -> delegationSpecifierAdditionalResolve(
                resolveSession,
                resolveElement,
                resolveElement.getParent() as KtClassOrObject,
                file,
                bodyResolveMode.bindingTraceFilter
            )

            is KtInitializerList -> delegationSpecifierAdditionalResolve(
                resolveSession,
                resolveElement,
                resolveElement.getParent() as KtEnumEntry,
                file,
                bodyResolveMode.bindingTraceFilter
            )

            is KtImportList -> {
                val resolver = resolveSession.fileScopeProvider.getImportResolver(resolveElement.getContainingKtFile())
                resolver.forceResolveNonDefaultImports()
                resolveSession.trace
            }

            is KtFileAnnotationList -> {
                val annotationEntry = resolveElement.annotationEntries.firstOrNull()
                if (annotationEntry != null) {
                    annotationAdditionalResolve(resolveSession, annotationEntry)
                } else {
                    resolveSession.trace
                }
            }

            is KtAnnotationEntry -> annotationAdditionalResolve(resolveSession, resolveElement)

            is KtTypeAlias -> typealiasAdditionalResolve(resolveSession, resolveElement, bodyResolveMode.bindingTraceFilter)

            is KtTypeParameter -> typeParameterAdditionalResolve(resolveSession, resolveElement)

            is KtTypeConstraint -> typeConstraintAdditionalResolve(resolveSession, resolveElement)

            is KtCodeFragment -> codeFragmentAdditionalResolve(resolveElement, bodyResolveMode)

            is KtScript -> scriptAdditionalResolve(resolveSession, resolveElement, bodyResolveMode.bindingTraceFilter)

            else -> {
                if (resolveElement.getParentOfType<KtPackageDirective>(true) != null) {
                    packageRefAdditionalResolve(resolveSession, resolveElement, bodyResolveMode.bindingTraceFilter)
                } else {
                    error("Invalid type of the topmost parent: $resolveElement\n${resolveElement.getElementTextWithContext()}")
                }
            }
        }

        if (bodyResolveMode.doControlFlowAnalysis) {
            val controlFlowTrace = DelegatingBindingTrace(
                trace.bindingContext, "Element control flow resolve", resolveElement, allowSliceRewrite = true
            )
            ControlFlowInformationProvider(
                resolveElement, controlFlowTrace, resolveElement.languageVersionSettings, resolveSession.platformDiagnosticSuppressor
            ).checkDeclaration()
            controlFlowTrace.addOwnDataTo(trace, null, false)
        }

        return Pair(trace.bindingContext, statementFilterUsed)
    }

    private fun packageRefAdditionalResolve(
        resolveSession: ResolveSession, ktElement: KtElement,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(ktElement, bindingTraceFilter)

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

    private fun codeFragmentAdditionalResolve(codeFragment: KtCodeFragment, bodyResolveMode: BodyResolveMode): BindingTrace {
        val contextResolveMode = if (bodyResolveMode == BodyResolveMode.PARTIAL)
            BodyResolveMode.PARTIAL_FOR_COMPLETION
        else
            bodyResolveMode

        return codeFragmentAnalyzer.analyzeCodeFragment(codeFragment, contextResolveMode)
    }

    private fun annotationAdditionalResolve(resolveSession: ResolveSession, ktAnnotationEntry: KtAnnotationEntry): BindingTrace {
        val modifierList = ktAnnotationEntry.getParentOfType<KtModifierList>(true)
        val declaration = modifierList?.getParentOfType<KtDeclaration>(true)
        if (declaration != null) {
            doResolveAnnotations(getAnnotationsByDeclaration(resolveSession, modifierList, declaration))
        } else {
            val fileAnnotationList = ktAnnotationEntry.getParentOfType<KtFileAnnotationList>(true)
            if (fileAnnotationList != null) {
                doResolveAnnotations(resolveSession.getFileAnnotations(fileAnnotationList.containingKtFile))
            }
            if (modifierList != null && modifierList.parent is KtFile) {
                doResolveAnnotations(resolveSession.getDanglingAnnotations(modifierList.containingKtFile))
            }
        }

        return resolveSession.trace
    }

    private fun doResolveAnnotations(annotations: Annotations) {
        ForceResolveUtil.forceResolveAllContents(annotations)
    }

    private fun getAnnotationsByDeclaration(
        resolveSession: ResolveSession,
        modifierList: KtModifierList,
        declaration: KtDeclaration
    ): Annotations {
        var descriptor = resolveSession.resolveToDescriptor(declaration)
        if (declaration is KtClass) {
            if (modifierList == declaration.primaryConstructorModifierList) {
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

    private fun delegationSpecifierAdditionalResolve(
        resolveSession: ResolveSession, ktElement: KtElement,
        classOrObject: KtClassOrObject, file: KtFile,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(ktElement, bindingTraceFilter)
        val descriptor = resolveSession.resolveToDescriptor(classOrObject) as LazyClassDescriptor

        // Activate resolving of supertypes
        ForceResolveUtil.forceResolveAllContents(descriptor.typeConstructor.supertypes)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE)
        bodyResolver.resolveSuperTypeEntryList(
            DataFlowInfo.EMPTY,
            classOrObject,
            descriptor,
            descriptor.unsubstitutedPrimaryConstructor,
            descriptor.scopeForConstructorHeaderResolution,
            descriptor.scopeForMemberDeclarationResolution
        )

        return trace
    }


    private fun destructuringDeclarationAdditionalResolve(
        resolveSession: ResolveSession,
        declaration: KtDestructuringDeclaration,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        for (entry in declaration.entries) {
            val descriptor = resolveSession.resolveToDescriptor(entry) as PropertyDescriptor
            ForceResolveUtil.forceResolveAllContents(descriptor)
            forceResolveAnnotationsInside(entry)
        }

        return createDelegatingTrace(declaration, bindingTraceFilter)
    }

    private fun propertyAdditionalResolve(
        resolveSession: ResolveSession, property: KtProperty,
        file: KtFile,
        statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(property, bindingTraceFilter)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        val descriptor = resolveSession.resolveToDescriptor(property) as PropertyDescriptor
        ForceResolveUtil.forceResolveAllContents(descriptor)

        val bodyResolveContext = BodyResolveContextForLazy(TopDownAnalysisMode.LocalDeclarations) { declaration ->
            assert(declaration.parent == property || declaration == property) {
                "Must be called only for property accessors or for property, but called for $declaration"
            }
            resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(declaration)
        }

        bodyResolver.resolveProperty(bodyResolveContext, property, descriptor)

        forceResolveAnnotationsInside(property)

        for (accessor in property.accessors) {
            ControlFlowInformationProvider(
                accessor, trace, accessor.languageVersionSettings, resolveSession.platformDiagnosticSuppressor
            ).checkDeclaration()
        }

        return trace
    }

    private fun scriptAdditionalResolve(
        resolveSession: ResolveSession, script: KtScript,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(script, bindingTraceFilter)
        val scriptDescriptor = resolveSession.resolveToDescriptor(script) as ScriptDescriptor
        ForceResolveUtil.forceResolveAllContents(scriptDescriptor)
        forceResolveAnnotationsInside(script)
        return trace
    }

    private fun functionAdditionalResolve(
        resolveSession: ResolveSession, namedFunction: KtNamedFunction, file: KtFile,
        statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(namedFunction, bindingTraceFilter)

        val scope = resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(namedFunction)
        val functionDescriptor = resolveSession.resolveToDescriptor(namedFunction) as FunctionDescriptor
        ForceResolveUtil.forceResolveAllContents(functionDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveFunctionBody(DataFlowInfo.EMPTY, trace, namedFunction, functionDescriptor, scope)

        forceResolveAnnotationsInside(namedFunction)

        return trace
    }

    private fun secondaryConstructorAdditionalResolve(
        resolveSession: ResolveSession, constructor: KtSecondaryConstructor,
        file: KtFile, statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(constructor, bindingTraceFilter)

        val scope = resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(constructor)
        val constructorDescriptor = resolveSession.resolveToDescriptor(constructor) as ClassConstructorDescriptor
        ForceResolveUtil.forceResolveAllContents(constructorDescriptor)

        val bodyResolver = createBodyResolver(resolveSession, trace, file, statementFilter)
        bodyResolver.resolveSecondaryConstructorBody(DataFlowInfo.EMPTY, trace, constructor, constructorDescriptor, scope)

        forceResolveAnnotationsInside(constructor)

        return trace
    }

    private fun constructorAdditionalResolve(
        resolveSession: ResolveSession,
        klass: KtClassOrObject,
        file: KtFile,
        filter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(klass, filter)

        val classDescriptor = resolveSession.resolveToDescriptor(klass) as ClassDescriptor
        val constructorDescriptor = classDescriptor.unsubstitutedPrimaryConstructor
                ?: error(
                    "Can't get primary constructor for descriptor '$classDescriptor' " +
                            "in from class '${klass.getElementTextWithContext()}'"
                )
        ForceResolveUtil.forceResolveAllContents(constructorDescriptor)

        val primaryConstructor = klass.primaryConstructor
        if (primaryConstructor != null) {
            val scope = resolveSession.declarationScopeProvider.getResolutionScopeForDeclaration(primaryConstructor)
            val bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE)
            bodyResolver.resolveConstructorParameterDefaultValues(
                DataFlowInfo.EMPTY,
                trace,
                primaryConstructor,
                constructorDescriptor,
                scope
            )

            forceResolveAnnotationsInside(primaryConstructor)
        }

        return trace
    }

    private fun typealiasAdditionalResolve(
        resolveSession: ResolveSession, typeAlias: KtTypeAlias,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(typeAlias, bindingTraceFilter)
        val typeAliasDescriptor = resolveSession.resolveToDescriptor(typeAlias)
        ForceResolveUtil.forceResolveAllContents(typeAliasDescriptor)
        forceResolveAnnotationsInside(typeAlias)
        return trace
    }

    private fun initializerAdditionalResolve(
        resolveSession: ResolveSession, anonymousInitializer: KtAnonymousInitializer,
        file: KtFile, statementFilter: StatementFilter,
        bindingTraceFilter: BindingTraceFilter
    ): BindingTrace {
        val trace = createDelegatingTrace(anonymousInitializer, bindingTraceFilter)

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
            file.jvmTarget,
            targetPlatform.findCompilerServices,
            file.languageVersionSettings
        ).get()
    }

    // All additional resolve should be done to separate trace
    private fun createDelegatingTrace(resolveElement: KtElement, filter: BindingTraceFilter): BindingTrace {
        return resolveSession.storageManager.createSafeTrace(
            DelegatingBindingTrace(
                resolveSession.bindingContext,
                "trace to resolve element",
                resolveElement,
                filter,
                allowSliceRewrite = true
            )
        )
    }

    private class BodyResolveContextForLazy(
        private val topDownAnalysisMode: TopDownAnalysisMode,
        private val declaringScopes: Function1<KtDeclaration, LexicalScope?>
    ) : BodiesResolveContext {
        override fun getFiles(): Collection<KtFile> = setOf()

        override fun getDeclaredClasses(): MutableMap<KtClassOrObject, ClassDescriptorWithResolutionScopes> = hashMapOf()

        override fun getAnonymousInitializers(): MutableMap<KtAnonymousInitializer, ClassDescriptorWithResolutionScopes> = hashMapOf()

        override fun getSecondaryConstructors(): MutableMap<KtSecondaryConstructor, ClassConstructorDescriptor> = hashMapOf()

        override fun getProperties(): MutableMap<KtProperty, PropertyDescriptor> = hashMapOf()

        override fun getFunctions(): MutableMap<KtNamedFunction, SimpleFunctionDescriptor> = hashMapOf()

        override fun getTypeAliases(): MutableMap<KtTypeAlias, TypeAliasDescriptor> = hashMapOf()

        override fun getDestructuringDeclarationEntries(): MutableMap<KtDestructuringDeclarationEntry, PropertyDescriptor> = hashMapOf()

        override fun getDeclaringScope(declaration: KtDeclaration): LexicalScope? = declaringScopes(declaration)

        override fun getScripts(): MutableMap<KtScript, ClassDescriptorWithResolutionScopes> = hashMapOf()

        override fun getOuterDataFlowInfo(): DataFlowInfo = DataFlowInfo.EMPTY

        override fun getTopDownAnalysisMode() = topDownAnalysisMode
    }
}

