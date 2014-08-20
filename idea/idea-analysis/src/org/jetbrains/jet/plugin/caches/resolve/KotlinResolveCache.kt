/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches.resolve

import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.project.Project
import org.jetbrains.jet.analyzer.AnalyzeExhaust
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils
import com.intellij.util.containers.SLRUCache
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.openapi.project.DumbService
import org.jetbrains.jet.plugin.util.ApplicationUtils
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm
import org.jetbrains.jet.context.SimpleGlobalContext
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.jet.asJava.LightClassUtil
import com.intellij.openapi.roots.libraries.LibraryUtil
import org.jetbrains.jet.lang.resolve.LibrarySourceHacks
import org.jetbrains.jet.plugin.project.TargetPlatform
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.analyzer.AnalyzerFacade
import java.util.HashMap
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetClassInitializer
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetImportDirective
import org.jetbrains.jet.lang.psi.JetAnnotationEntry
import org.jetbrains.jet.lang.psi.JetTypeConstraint
import org.jetbrains.jet.lang.psi.JetPackageDirective
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.resolve.CompositeBindingContext
import org.jetbrains.jet.lang.psi.JetParameter
import org.jetbrains.jet.lang.psi.JetDelegationSpecifierList
import org.jetbrains.jet.lang.psi.JetTypeParameter
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.lang.psi.JetCallableDeclaration
import org.jetbrains.jet.lang.psi.JetCodeFragment
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo
import org.jetbrains.jet.analyzer.analyzeInContext
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope

public trait CacheExtension<T> {
    val platform: TargetPlatform
    fun getData(setup: AnalyzerFacade.Setup): T
}

private class SessionAndSetup(
        val platform: TargetPlatform,
        val resolveSessionForBodies: ResolveSessionForBodies,
        val setup: AnalyzerFacade.Setup
)

private class KotlinResolveCache(
        val project: Project,
        setupProvider: () -> CachedValueProvider.Result<SessionAndSetup>
) {

    private val setupCache = SynchronizedCachedValue(project, setupProvider, trackValue = false)

    public fun getLazyResolveSession(): ResolveSessionForBodies = setupCache.getValue().resolveSessionForBodies

    public fun <T> get(extension: CacheExtension<T>): T {
        val sessionAndSetup = setupCache.getValue()
        assert(extension.platform == sessionAndSetup.platform,
               "Extension $extension declares platfrom ${extension.platform} which is incompatible with ${sessionAndSetup.platform}")
        return extension.getData(sessionAndSetup.setup)
    }

    private val analysisResults = CachedValuesManager.getManager(project).createCachedValue ({
        val resolveSession = getLazyResolveSession()
        val results = object : SLRUCache<JetFile, PerFileAnalysisCache>(2, 3) {
            override fun createValue(file: JetFile?): PerFileAnalysisCache {
                return PerFileAnalysisCache(file!!, resolveSession)
            }
        }

        CachedValueProvider.Result(results, PsiModificationTracker.MODIFICATION_COUNT, resolveSession.getExceptionTracker())
    }, false)

    fun getAnalysisResultsForElements(elements: Collection<JetElement>): AnalyzeExhaust {
        val slruCache = synchronized(analysisResults) {
            analysisResults.getValue()!!
        }
        val results = elements.map {
            val perFileCache = synchronized(slruCache) {
                slruCache[it.getContainingJetFile()]
            }
            perFileCache.getAnalysisResults(it)
        }
        val error = results.firstOrNull { it.isError() }
        val bindingContext = CompositeBindingContext.create(results.map { it.getBindingContext() })
        return if (error != null)
                   AnalyzeExhaust.error(bindingContext, error.getError())
               else
                   AnalyzeExhaust.success(bindingContext, getLazyResolveSession().getModuleDescriptor())
    }
}

private class PerFileAnalysisCache(val file: JetFile, val resolveSession: ResolveSessionForBodies) {
    private val cache = HashMap<PsiElement, AnalyzeExhaust>()

    private fun lookUp(analyzableElement: JetElement): AnalyzeExhaust? {
        // Looking for parent elements that are already analyzed
        // Also removing all elements whose parents are already analyzed, to guarantee consistency
        val descendantsOfCurrent = arrayListOf<PsiElement>()
        val toRemove = hashSetOf<PsiElement>()

        var current: PsiElement? = analyzableElement
        var result: AnalyzeExhaust? = null
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

    fun getAnalysisResults(element: JetElement): AnalyzeExhaust {
        assert (element.getContainingJetFile() == file, "Wrong file. Expected $file, but was ${element.getContainingJetFile()}")

        val analyzableParent = KotlinResolveDataProvider.findAnalyzableParent(element)

        return synchronized(this) { (): AnalyzeExhaust ->

            val cached = lookUp(analyzableParent)
            if (cached != null) return@synchronized cached

            val result = analyze(analyzableParent)

            cache[analyzableParent] = result

            return@synchronized result
        }
    }

    private fun analyze(analyzableElement: JetElement): AnalyzeExhaust {
        val project = analyzableElement.getProject()
        if (DumbService.isDumb(project)) {
            return AnalyzeExhaust.EMPTY
        }

        ApplicationUtils.warnTimeConsuming(LOG)

        try {
            return KotlinResolveDataProvider.analyze(project, resolveSession, analyzableElement)
        }
        catch (e: ProcessCanceledException) {
            throw e
        }
        catch (e: Throwable) {
            DiagnosticUtils.throwIfRunningOnServer(e)
            LOG.error(e)

            return AnalyzeExhaust.error(BindingContext.EMPTY, e)
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

    fun analyze(project: Project, resolveSession: ResolveSessionForBodies, analyzableElement: JetElement): AnalyzeExhaust {
        try {
            if (analyzableElement is JetCodeFragment) {
                return AnalyzeExhaust.success(
                        analyzeExpressionCodeFragment(resolveSession, analyzableElement),
                        resolveSession.getModuleDescriptor()
                )
            }

            val file = analyzableElement.getContainingJetFile()
            val virtualFile = file.getVirtualFile()
            if (LightClassUtil.belongsToKotlinBuiltIns(file)
                || virtualFile != null && LibraryUtil.findLibraryEntry(virtualFile, file.getProject()) != null) {
                // Library sources: mark file to skip
                file.putUserData(LibrarySourceHacks.SKIP_TOP_LEVEL_MEMBERS, true)
            }

            val trace = DelegatingBindingTrace(resolveSession.getBindingContext(), "Trace for resolution of " + analyzableElement)
            val injector = InjectorForTopDownAnalyzerForJvm(
                    project,
                    SimpleGlobalContext(resolveSession.getStorageManager(), resolveSession.getExceptionTracker()),
                    trace,
                    resolveSession.getModuleDescriptor()
            )
            injector.getLazyTopDownAnalyzer()!!.analyzeDeclarations(
                    resolveSession,
                    TopDownAnalysisParameters.createForLazy(
                            resolveSession.getStorageManager(),
                            resolveSession.getExceptionTracker(),
                            /* analyzeCompletely = */ { true },
                            /* analyzingBootstrapLibrary = */ false,
                            /* declaredLocally = */ false
                    ),
                    listOf(analyzableElement)
            )
            return AnalyzeExhaust.success(
                    trace.getBindingContext(),
                    resolveSession.getModuleDescriptor()
            )
        }
        catch (e: ProcessCanceledException) {
            throw e
        }
        catch (e: Throwable) {
            DiagnosticUtils.throwIfRunningOnServer(e)
            LOG.error(e)

            return AnalyzeExhaust.error(BindingContext.EMPTY, e)
        }
    }

    private fun analyzeExpressionCodeFragment(resolveSession: ResolveSessionForBodies, codeFragment: JetCodeFragment): BindingContext {
        val codeFragmentExpression = codeFragment.getContentElement()
        if (codeFragmentExpression !is JetExpression) return BindingContext.EMPTY

        val contextElement = codeFragment.getContext()
        if (contextElement !is JetExpression) return BindingContext.EMPTY

        val contextForElement = contextElement.getBindingContext()

        val scopeForContextElement = contextForElement[BindingContext.RESOLUTION_SCOPE, contextElement]
        if (scopeForContextElement == null) return BindingContext.EMPTY

        val codeFragmentScope = resolveSession.getScopeProvider().getFileScope(codeFragment)
        val chainedScope = ChainedScope(
                                scopeForContextElement.getContainingDeclaration(),
                                "Scope for resolve code fragment",
                                scopeForContextElement, codeFragmentScope)

        val dataFlowInfoForContextElement = contextForElement[BindingContext.EXPRESSION_DATA_FLOW_INFO, contextElement]
        val dataFlowInfo = dataFlowInfoForContextElement ?: DataFlowInfo.EMPTY
        return codeFragmentExpression.analyzeInContext(
                chainedScope,
                BindingTraceContext(),
                dataFlowInfo,
                TypeUtils.NO_EXPECTED_TYPE,
                resolveSession.getModuleDescriptor()
        )
    }
}
