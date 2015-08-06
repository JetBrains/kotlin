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

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.SimpleGlobalContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.context.withProject
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.frontend.di.createContainerForLazyBodyResolve
import org.jetbrains.kotlin.idea.project.ResolveSessionForBodies
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.util.getScopeAndDataFlowForAnalyzeFragment
import org.jetbrains.kotlin.types.TypeUtils
import java.util.HashMap

public interface CacheExtension<T> {
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

    public fun getLazyResolveSession(moduleDescriptor: ModuleDescriptor): ResolveSessionForBodies {
        return moduleResolverProvider.resolveSessionForBodiesByDescriptor(moduleDescriptor)
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
        assert(elements.isNotEmpty(), "elements collection should not be empty")
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

        var result: AnalysisResult? = null
        for (current in analyzableElement.parentsWithSelf) {
            val cached = cache[current]
            if (cached != null) {
                result = cached
                toRemove.addAll(descendantsOfCurrent)
                descendantsOfCurrent.clear()
            }

            descendantsOfCurrent.add(current)
        }

        cache.keySet().removeAll(toRemove)

        return result
    }

    fun getAnalysisResults(element: JetElement): AnalysisResult {
        assert (element.getContainingJetFile() == file) { "Wrong file. Expected $file, but was ${element.getContainingJetFile()}" }

        val analyzableParent = KotlinResolveDataProvider.findAnalyzableParent(element)

        return synchronized<AnalysisResult>(this) {

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
    private val topmostElementTypes = arrayOf<Class<out PsiElement?>?>(
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
        if (element is JetFile) return element

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
            val module = resolveSession.getModuleDescriptor()
            if (analyzableElement is JetCodeFragment) {
                return AnalysisResult.success(analyzeExpressionCodeFragment(resolveSession, analyzableElement), module)
            }

            val file = analyzableElement.getContainingJetFile()
            if (LightClassUtil.belongsToKotlinBuiltIns(file) || file.getModuleInfo() is LibrarySourceInfo) {
                // Library sources: mark file to skip
                file.putUserData(LibrarySourceHacks.SKIP_TOP_LEVEL_MEMBERS, true)
            }

            val trace = DelegatingBindingTrace(resolveSession.getBindingContext(), "Trace for resolution of " + analyzableElement)

            val targetPlatform = TargetPlatformDetector.getPlatform(analyzableElement.getContainingJetFile())
            val globalContext = SimpleGlobalContext(resolveSession.getStorageManager(), resolveSession.getExceptionTracker())
            val moduleContext = globalContext.withProject(project).withModule(module)
            val lazyTopDownAnalyzer = createContainerForLazyBodyResolve(
                    moduleContext,
                    resolveSession,
                    trace,
                    targetPlatform,
                    resolveSession.getBodyResolveCache()
            ).get<LazyTopDownAnalyzerForTopLevel>()

            lazyTopDownAnalyzer.analyzeDeclarations(
                    TopDownAnalysisMode.TopLevelDeclarations,
                    listOf(analyzableElement)
            )
            return AnalysisResult.success(
                    trace.getBindingContext(),
                    module
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

        val (scopeForContextElement, dataFlowInfo) = codeFragment.getScopeAndDataFlowForAnalyzeFragment(resolveSession) {
            resolveSession.resolveToElement(it, BodyResolveMode.PARTIAL_FOR_COMPLETION) //TODO: discuss it
        } ?: return BindingContext.EMPTY


        return codeFragmentExpression.analyzeInContext(
                scopeForContextElement,
                BindingTraceContext(),
                dataFlowInfo,
                TypeUtils.NO_EXPECTED_TYPE,
                resolveSession.getModuleDescriptor()
        )
    }
}
