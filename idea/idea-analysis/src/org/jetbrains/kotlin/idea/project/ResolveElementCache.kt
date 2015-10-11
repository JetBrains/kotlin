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

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.asJava.KotlinCodeBlockModificationListener
import org.jetbrains.kotlin.cfg.JetFlowInformationProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.frontend.di.createContainerForBodyResolve
import org.jetbrains.kotlin.idea.caches.resolve.CodeFragmentAnalyzer
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.JetProbablyNothingPropertyShortNameIndex
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.utils.addToStdlib.check

public class ResolveElementCache(
        private val resolveSession: ResolveSession,
        private val project: Project,
        private val targetPlatform: TargetPlatform,
        private val codeFragmentAnalyzer: CodeFragmentAnalyzer
) : BodyResolveCache {
    private class CachedFullResolve(val bindingContext: BindingContext, resolveElement: JetElement) {
        private val modificationStamp: Long? = modificationStamp(resolveElement)

        fun isUpToDate(resolveElement: JetElement) = modificationStamp == modificationStamp(resolveElement)

        private fun modificationStamp(resolveElement: JetElement): Long? {
            val file = resolveElement.containingFile
            return if (!file.isPhysical) // for non-physical file we don't get OUT_OF_CODE_BLOCK_MODIFICATION_COUNT increased and must reset data on any modification of the file
                file.modificationStamp
            else if (resolveElement is JetDeclaration && KotlinCodeBlockModificationListener.isBlockDeclaration(resolveElement))
                resolveElement.getModificationStamp()
            else
                null
        }
    }

    // drop whole cache after change "out of code block"
    private val fullResolveCache: CachedValue<MutableMap<JetElement, CachedFullResolve>> = CachedValuesManager.getManager(project).createCachedValue(
            object : CachedValueProvider<MutableMap<JetElement, CachedFullResolve>> {
                override fun compute(): CachedValueProvider.Result<MutableMap<JetElement, CachedFullResolve>> {
                    return CachedValueProvider.Result.create(ContainerUtil.createConcurrentSoftValueMap<JetElement, CachedFullResolve>(),
                                                             PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
                                                             resolveSession.exceptionTracker)
                }
            },
            false)

    private class CachedPartialResolve(val bindingContext: BindingContext, file: JetFile) {
        private val modificationStamp: Long? = modificationStamp(file)

        fun isUpToDate(file: JetFile) = modificationStamp == modificationStamp(file)

        private fun modificationStamp(file: JetFile): Long? {
            return if (!file.isPhysical) // for non-physical file we don't get MODIFICATION_COUNT increased and must reset data on any modification of the file
                file.modificationStamp
            else
                null
        }
    }

    private val partialBodyResolveCache: CachedValue<MutableMap<JetExpression, CachedPartialResolve>> = CachedValuesManager.getManager(project).createCachedValue(
            object : CachedValueProvider<MutableMap<JetExpression, CachedPartialResolve>> {
                override fun compute(): CachedValueProvider.Result<MutableMap<JetExpression, CachedPartialResolve>> {
                    return CachedValueProvider.Result.create(ContainerUtil.createConcurrentSoftValueMap<JetExpression, CachedPartialResolve>(),
                                                             PsiModificationTracker.MODIFICATION_COUNT,
                                                             resolveSession.exceptionTracker)
                }
            },
            false)


    private fun probablyNothingCallableNames(): ProbablyNothingCallableNames {
        return object : ProbablyNothingCallableNames {
            override fun functionNames() = JetProbablyNothingFunctionShortNameIndex.getInstance().getAllKeys(project)
            override fun propertyNames() = JetProbablyNothingPropertyShortNameIndex.getInstance().getAllKeys(project)
        }
    }

    override fun resolveFunctionBody(function: JetNamedFunction)
            = getElementAdditionalResolve(function, function, BodyResolveMode.FULL)


    fun getElementAdditionalResolve(resolveElement: JetElement, contextElement: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
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
                if (resolveElement !is JetDeclaration) {
                    return getElementAdditionalResolve(resolveElement, contextElement, BodyResolveMode.FULL)
                }

                val file = resolveElement.getContainingJetFile()
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
                if (resolveElement !is JetDeclaration) {
                    return getElementAdditionalResolve(resolveElement, contextElement, BodyResolveMode.FULL)
                }

                // not cached
                return performElementAdditionalResolve(resolveElement, contextElement, bodyResolveMode).first
            }
        }
    }

    public fun resolveToElement(element: JetElement, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext {
        var contextElement = element

        val elementOfAdditionalResolve = findElementOfAdditionalResolve(contextElement)

        if (elementOfAdditionalResolve is JetParameter) {
            // Parameters for function literal could be met inside other parameters. We can't make resolveToDescriptors for internal elements.
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
                javaClass<JetImportList>(),
                javaClass<JetAnnotationEntry>(),
                javaClass<JetTypeParameter>(),
                javaClass<JetTypeConstraint>(),
                javaClass<JetPackageDirective>(),
                javaClass<JetCodeFragment>()) as JetElement?

        when (elementOfAdditionalResolve) {
            null -> {
                // Case of JetAnnotationEntry on top level class
                if (element is JetAnnotationEntry) {
                    return element
                }

                return null
            }

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

    private fun performElementAdditionalResolve(resolveElement: JetElement, contextElement: JetElement, bodyResolveMode: BodyResolveMode): Pair<BindingContext, StatementFilter> {
        val file = resolveElement.getContainingJetFile()

        var statementFilterUsed = StatementFilter.NONE

        fun createStatementFilter(): StatementFilter {
            assert(resolveElement is JetDeclaration)
            if (bodyResolveMode != BodyResolveMode.FULL) {
                statementFilterUsed = PartialBodyResolveFilter(
                        contextElement,
                        resolveElement as JetDeclaration,
                        probablyNothingCallableNames(),
                        bodyResolveMode == BodyResolveMode.PARTIAL_FOR_COMPLETION)
            }
            return statementFilterUsed
        }

        val trace: BindingTrace = when (resolveElement) {
            is JetNamedFunction -> functionAdditionalResolve(resolveSession, resolveElement, file, createStatementFilter())

            is JetClassInitializer -> initializerAdditionalResolve(resolveSession, resolveElement, file, createStatementFilter())

            is JetSecondaryConstructor -> secondaryConstructorAdditionalResolve(resolveSession, resolveElement, file, createStatementFilter())

            is JetProperty -> propertyAdditionalResolve(resolveSession, resolveElement, file, createStatementFilter())

            is JetDelegationSpecifierList -> delegationSpecifierAdditionalResolve(resolveSession, resolveElement, resolveElement.getParent() as JetClassOrObject, file)

            is JetInitializerList -> delegationSpecifierAdditionalResolve(resolveSession, resolveElement, resolveElement.getParent() as JetEnumEntry, file)

            is JetImportList -> {
                val scope = resolveSession.getFileScopeProvider().getFileScope(resolveElement.getContainingJetFile())
                scope.forceResolveAllImports()
                resolveSession.trace
            }

            is JetAnnotationEntry -> annotationAdditionalResolve(resolveSession, resolveElement)

            is JetClass -> constructorAdditionalResolve(resolveSession, resolveElement, file)

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

        return Pair(trace.getBindingContext(), statementFilterUsed)
    }

    private fun packageRefAdditionalResolve(resolveSession: ResolveSession, jetElement: JetElement): BindingTrace {
        val trace = createDelegatingTrace(jetElement)

        if (jetElement is JetSimpleNameExpression) {
            val header = jetElement.getParentOfType<JetPackageDirective>(true)!!

            if (trace.getBindingContext()[BindingContext.RESOLUTION_SCOPE, jetElement] == null) {
                val scope = resolveSession.getModuleDescriptor().getPackage(header.getFqName(jetElement).parent()).memberScope
                trace.record(BindingContext.RESOLUTION_SCOPE, jetElement, scope)
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

        return resolveSession.trace
    }

    private fun codeFragmentAdditionalResolve(resolveSession: ResolveSession, codeFragment: JetCodeFragment, bodyResolveMode: BodyResolveMode): BindingTrace {
        val trace = createDelegatingTrace(codeFragment)

        val contextResolveMode = if (bodyResolveMode == BodyResolveMode.PARTIAL)
            BodyResolveMode.PARTIAL_FOR_COMPLETION
        else
            bodyResolveMode
        codeFragmentAnalyzer.analyzeCodeFragment(codeFragment, trace, contextResolveMode)

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

        return resolveSession.trace
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

        return resolveSession.trace
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

    private fun constructorAdditionalResolve(resolveSession: ResolveSession, klass: JetClass, file: JetFile): BindingTrace {
        val trace = createDelegatingTrace(klass)
        val scope = resolveSession.getDeclarationScopeProvider().getResolutionScopeForDeclaration(klass)

        val classDescriptor = resolveSession.resolveToDescriptor(klass) as ClassDescriptor
        val constructorDescriptor = classDescriptor.getUnsubstitutedPrimaryConstructor()
                                    ?: error("Can't get primary constructor for descriptor '$classDescriptor' in from class '${klass.getElementTextWithContext()}'")

        val bodyResolver = createBodyResolver(resolveSession, trace, file, StatementFilter.NONE)
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
        val globalContext = SimpleGlobalContext(resolveSession.storageManager, resolveSession.getExceptionTracker())
        val module = resolveSession.getModuleDescriptor()
        return createContainerForBodyResolve(
                globalContext.withProject(file.getProject()).withModule(module),
                trace,
                targetPlatform,
                statementFilter
        ).get<BodyResolver>()
    }

    // All additional resolve should be done to separate trace
    private fun createDelegatingTrace(resolveElement: JetElement): BindingTrace {
        return resolveSession.storageManager.createSafeTrace(
                DelegatingBindingTrace(resolveSession.getBindingContext(), "trace to resolve element", resolveElement))
    }

    private class BodyResolveContextForLazy(
            private val topDownAnalysisMode: TopDownAnalysisMode,
            private val declaringScopes: Function1<JetDeclaration, LexicalScope?>
    ) : BodiesResolveContext {
        override fun getFiles(): Collection<JetFile> = setOf()

        override fun getDeclaredClasses(): MutableMap<JetClassOrObject, ClassDescriptorWithResolutionScopes> = hashMapOf()

        override fun getAnonymousInitializers(): MutableMap<JetClassInitializer, ClassDescriptorWithResolutionScopes> = hashMapOf()

        override fun getSecondaryConstructors(): MutableMap<JetSecondaryConstructor, ConstructorDescriptor> = hashMapOf()

        override fun getProperties(): MutableMap<JetProperty, PropertyDescriptor> = hashMapOf()

        override fun getFunctions(): MutableMap<JetNamedFunction, SimpleFunctionDescriptor> = hashMapOf()

        override fun getDeclaringScope(declaration: JetDeclaration): LexicalScope? = declaringScopes(declaration)

        override fun getScripts(): MutableMap<JetScript, ScriptDescriptor> = hashMapOf()

        override fun getOuterDataFlowInfo(): DataFlowInfo = DataFlowInfo.EMPTY

        override fun getTopDownAnalysisMode() = topDownAnalysisMode
    }
}

