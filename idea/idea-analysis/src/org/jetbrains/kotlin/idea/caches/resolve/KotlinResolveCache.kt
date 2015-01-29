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

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import com.intellij.util.containers.SLRUCache
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.openapi.project.DumbService
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.resolve.TopDownAnalysisParameters
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.resolve.LibrarySourceHacks
import org.jetbrains.kotlin.idea.project.TargetPlatform
import org.jetbrains.kotlin.idea.project.ResolveSessionForBodies
import java.util.HashMap
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetClassInitializer
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.JetAnnotationEntry
import org.jetbrains.kotlin.psi.JetTypeConstraint
import org.jetbrains.kotlin.psi.JetPackageDirective
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.resolve.CompositeBindingContext
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetDelegationSpecifierList
import org.jetbrains.kotlin.psi.JetTypeParameter
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.di.InjectorForLazyBodyResolve
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import com.intellij.openapi.project.IndexNotReadyException
import org.jetbrains.kotlin.psi.JetBlockExpression

public trait CacheExtension<T> {
    public val platform: TargetPlatform
    public fun getData(resolverProvider: ModuleResolverProvider): T
}

private class KotlinResolveCache(
        val project: Project,
        computeModuleResolverProvider: () -> CachedValueProvider.Result<ModuleResolverProvider>
) {

    private val resolverCache = SynchronizedCachedValue(project, computeModuleResolverProvider, trackValue = false)

    val moduleResolverProvider: ModuleResolverProvider
        get() = resolverCache.getValue()

    public fun getLazyResolveSession(element: JetElement): ResolveSessionForBodies {
        return moduleResolverProvider.resolveSessionForBodiesByModule(element.getModuleInfo())
    }

    public fun <T> get(extension: CacheExtension<T>): T {
        return extension.getData(moduleResolverProvider)
    }

    private val analysisResults = CachedValuesManager.getManager(project).createCachedValue(
    {
        val resolverProvider = moduleResolverProvider
        val results = object : SLRUCache<JetFile, PerFileAnalysisCache>(2, 3) {
            override fun createValue(file: JetFile?): PerFileAnalysisCache {
                return PerFileAnalysisCache(file!!, resolverProvider.resolveSessionForBodiesByModule(file.getModuleInfo()))
            }
        }
        CachedValueProvider.Result(results, PsiModificationTracker.MODIFICATION_COUNT, resolverProvider.exceptionTracker)
    }, false)

    fun getAnalysisResultsForElements(elements: Collection<JetElement>): AnalysisResult {
        val slruCache = synchronized(analysisResults) {
            analysisResults.getValue()!!
        }
        val results = elements.map {
            val perFileCache = synchronized(slruCache) {
                slruCache[it.getContainingJetFile()]
            }
            perFileCache.getAnalysisResults(it)
        }
        val withError = results.firstOrNull { it.isError() }
        val bindingContext = CompositeBindingContext.create(results.map { it.bindingContext })
        return if (withError != null)
                   AnalysisResult.error(bindingContext, withError.error)
               else
                    //TODO: (module refactoring) several elements are passed here in debugger
                   AnalysisResult.success(bindingContext, getLazyResolveSession(elements.first()).getModuleDescriptor())
    }
}

private class PerFileAnalysisCache(val file: JetFile, val resolveSession: ResolveSessionForBodies) {
    private val cache = HashMap<PsiElement, AnalysisResult>()

    private fun lookUp(analyzableElement: JetElement): AnalysisResult? {
        // Looking for parent elements that are already analyzed
        // Also removing all elements whose parents are already analyzed, to guarantee consistency
        val descendantsOfCurrent = arrayListOf<PsiElement>()
        val toRemove = hashSetOf<PsiElement>()

        var current: PsiElement? = analyzableElement
        var result: AnalysisResult? = null
        while (current != null) {
            val cached = cache[current]
            if (cached != null) {
                result = cached
                toRemove.addAll(descendantsOfCurrent)
                descendantsOfCurrent.clear()
            }

            descendantsOfCurrent.add(current!!)
            current = current!!.getParent()
        }

        cache.keySet().removeAll(toRemove)

        return result
    }

    fun getAnalysisResults(element: JetElement): AnalysisResult {
        assert (element.getContainingJetFile() == file, "Wrong file. Expected $file, but was ${element.getContainingJetFile()}")

        val analyzableParent = KotlinResolveDataProvider.findAnalyzableParent(element)

        return synchronized(this) { (): AnalysisResult ->

            val cached = lookUp(analyzableParent)
            if (cached != null) return@synchronized cached

            val result = analyze(analyzableParent)

            cache[analyzableParent] = result

            return@synchronized result
        }
    }

    private fun analyze(analyzableElement: JetElement): AnalysisResult {
        val project = analyzableElement.getProject()
        if (DumbService.isDumb(project)) {
            return AnalysisResult.EMPTY
        }

        try {
            return KotlinResolveDataProvider.analyze(project, resolveSession, analyzableElement)
        }
        catch (e: ProcessCanceledException) {
            throw e
        }
        catch (e: IndexNotReadyException) {
            throw e
        }
        catch (e: Throwable) {
            DiagnosticUtils.throwIfRunningOnServer(e)
            LOG.error(e)

            return AnalysisResult.error(BindingContext.EMPTY, e)
        }
    }
}

private object KotlinResolveDataProvider {
    private val topmostElementTypes = array<Class<out PsiElement?>?>(
            javaClass<JetNamedFunction>(),
            javaClass<JetClassInitializer>(),
            javaClass<JetProperty>(),
            javaClass<JetImportDirective>(),
            javaClass<JetPackageDirective>(),
            javaClass<JetCodeFragment>(),
            // TODO: Non-analyzable so far, add more granular analysis
            javaClass<JetAnnotationEntry>(),
            javaClass<JetTypeConstraint>(),
            javaClass<JetDelegationSpecifierList>(),
            javaClass<JetTypeParameter>(),
            javaClass<JetParameter>()
    )

    fun findAnalyzableParent(element: JetElement): JetElement {
        val topmostElement = JetPsiUtil.getTopmostParentOfTypes(element, *topmostElementTypes) as JetElement?
        // parameters and supertype lists are not analyzable by themselves, but if we don't count them as topmost, we'll stop inside, say,
        // object expressions inside arguments of super constructors of classes (note that classes themselves are not topmost elements)
        val analyzableElement = when (topmostElement) {
            is JetAnnotationEntry,
            is JetTypeConstraint,
            is JetDelegationSpecifierList,
            is JetTypeParameter,
            is JetParameter -> PsiTreeUtil.getParentOfType(topmostElement, javaClass<JetClassOrObject>(), javaClass<JetCallableDeclaration>())
            else -> topmostElement
        }
        return analyzableElement
                    // if none of the above worked, take the outermost declaration
                    ?: PsiTreeUtil.getTopmostParentOfType(element, javaClass<JetDeclaration>())
                    // if even that didn't work, take the whole file
                    ?: element.getContainingJetFile()
    }

    fun analyze(project: Project, resolveSession: ResolveSessionForBodies, analyzableElement: JetElement): AnalysisResult {
        try {
            if (analyzableElement is JetCodeFragment) {
                return AnalysisResult.success(
                        analyzeExpressionCodeFragment(resolveSession, analyzableElement),
                        resolveSession.getModuleDescriptor()
                )
            }

            val file = analyzableElement.getContainingJetFile()
            if (LightClassUtil.belongsToKotlinBuiltIns(file) || file.getModuleInfo() is LibrarySourceInfo) {
                // Library sources: mark file to skip
                file.putUserData(LibrarySourceHacks.SKIP_TOP_LEVEL_MEMBERS, true)
            }

            val trace = DelegatingBindingTrace(resolveSession.getBindingContext(), "Trace for resolution of " + analyzableElement)

            val targetPlatform = TargetPlatformDetector.getPlatform(analyzableElement.getContainingJetFile())
            val lazyTopDownAnalyzer = InjectorForLazyBodyResolve(
                    project,
                    SimpleGlobalContext(resolveSession.getStorageManager(), resolveSession.getExceptionTracker()),
                    resolveSession,
                    trace,
                    targetPlatform.getAdditionalCheckerProvider(),
                    targetPlatform.getDynamicTypesSettings()
            ).getLazyTopDownAnalyzerForTopLevel()!!

            lazyTopDownAnalyzer.analyzeDeclarations(
                    TopDownAnalysisParameters.create(resolveSession.getStorageManager(), resolveSession.getExceptionTracker(), false, false),
                    listOf(analyzableElement)
            )
            return AnalysisResult.success(
                    trace.getBindingContext(),
                    resolveSession.getModuleDescriptor()
            )
        }
        catch (e: ProcessCanceledException) {
            throw e
        }
        catch (e: IndexNotReadyException) {
            throw e
        }
        catch (e: Throwable) {
            DiagnosticUtils.throwIfRunningOnServer(e)
            LOG.error(e)

            return AnalysisResult.error(BindingContext.EMPTY, e)
        }
    }

    private fun analyzeExpressionCodeFragment(resolveSession: ResolveSessionForBodies, codeFragment: JetCodeFragment): BindingContext {
        val codeFragmentExpression = codeFragment.getContentElement()
        if (codeFragmentExpression !is JetExpression) return BindingContext.EMPTY

        val contextElement = codeFragment.getContext()

        val scopeForContextElement: JetScope?
        val dataFlowInfo: DataFlowInfo
        if (contextElement is JetClassOrObject) {
            val descriptor = resolveSession.resolveToDescriptor(contextElement) as LazyClassDescriptor

            scopeForContextElement = descriptor.getScopeForMemberDeclarationResolution()
            dataFlowInfo = DataFlowInfo.EMPTY
        }
        else if (contextElement is JetBlockExpression) {
            val newContextElement = contextElement.getStatements().lastOrNull()
            if (newContextElement !is JetExpression) return BindingContext.EMPTY

            val contextForElement = newContextElement.getResolutionFacade().analyze(newContextElement, BodyResolveMode.FULL)

            scopeForContextElement = contextForElement[BindingContext.RESOLUTION_SCOPE, newContextElement]
            dataFlowInfo = contextForElement.getDataFlowInfo(newContextElement)
        }
        else {
            if (contextElement !is JetExpression) return BindingContext.EMPTY

            val contextForElement = contextElement.getResolutionFacade().analyze(contextElement, BodyResolveMode.PARTIAL_FOR_COMPLETION) //TODO: discuss it

            scopeForContextElement = contextForElement[BindingContext.RESOLUTION_SCOPE, contextElement]
            dataFlowInfo = contextForElement.getDataFlowInfo(contextElement)
        }

        if (scopeForContextElement == null) return BindingContext.EMPTY

        val codeFragmentScope = resolveSession.getScopeProvider().getFileScope(codeFragment)
        val chainedScope = ChainedScope(
                                scopeForContextElement.getContainingDeclaration(),
                                "Scope for resolve code fragment",
                                scopeForContextElement, codeFragmentScope)

        return codeFragmentExpression.analyzeInContext(
                chainedScope,
                BindingTraceContext(),
                dataFlowInfo,
                TypeUtils.NO_EXPECTED_TYPE,
                resolveSession.getModuleDescriptor()
        )
    }
}
