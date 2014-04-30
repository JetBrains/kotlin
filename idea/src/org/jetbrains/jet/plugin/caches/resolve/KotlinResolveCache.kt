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
import org.jetbrains.jet.descriptors.serialization.descriptors.MemberFilter
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters
import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.jet.asJava.LightClassUtil
import com.intellij.openapi.roots.libraries.LibraryUtil
import org.jetbrains.jet.lang.resolve.LibrarySourceHacks
import org.jetbrains.jet.plugin.project.TargetPlatform
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.analyzer.AnalyzerFacade

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

    private data class Task(
            val elements: Set<JetElement>
    )

    private val analysisResults = CachedValuesManager.getManager(project).createCachedValue ({
        val resolveSession = getLazyResolveSession()
        val results = object : SLRUCache<Task, AnalyzeExhaust>(2, 3) {
            override fun createValue(task: Task?): AnalyzeExhaust {
                if (DumbService.isDumb(project)) {
                    return AnalyzeExhaust.EMPTY
                }

                ApplicationUtils.warnTimeConsuming(LOG)

                try {
                    for (element in task!!.elements) {
                        val file = element.getContainingJetFile()
                        val virtualFile = file.getVirtualFile()
                        if (LightClassUtil.belongsToKotlinBuiltIns(file)
                            || virtualFile != null && LibraryUtil.findLibraryEntry(virtualFile, file.getProject()) != null) {
                            // Library sources: mark file to skip
                            file.putUserData(LibrarySourceHacks.SKIP_TOP_LEVEL_MEMBERS, true)
                        }
                    }

                    // todo: look for pre-existing results for this element or its parents
                    val trace = DelegatingBindingTrace(resolveSession.getBindingContext(), "Trace for resolution of " + task.elements.makeString(", "))
                    val injector = InjectorForTopDownAnalyzerForJvm(
                            project,
                            SimpleGlobalContext(resolveSession.getStorageManager(), resolveSession.getExceptionTracker()),
                            trace,
                            resolveSession.getModuleDescriptor(),
                            MemberFilter.ALWAYS_TRUE
                    )
                    injector.getLazyTopDownAnalyzer()!!.analyzeDeclarations(
                            resolveSession,
                            TopDownAnalysisParameters.createForLazy(
                                    resolveSession.getStorageManager(),
                                    resolveSession.getExceptionTracker(),
                                    analyzeCompletely = { true },
                                    analyzingBootstrapLibrary = false,
                                    declaredLocally = false
                            ),
                            task.elements.map { getContainingNonlocalDeclaration(it) }
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
                    handleError(e)

                    val bindingTraceContext = BindingTraceContext()
                    return AnalyzeExhaust.error(bindingTraceContext.getBindingContext(), e)
                }
            }
        }

        CachedValueProvider.Result(results, PsiModificationTracker.MODIFICATION_COUNT, resolveSession.getExceptionTracker())
    }, false)

    private fun getAnalysisResults(task: Task): AnalyzeExhaust {
        val slruCache = synchronized(analysisResults) {
            analysisResults.getValue()!!
        }
        return synchronized(slruCache) {
            slruCache[task]
        }
    }

    fun getAnalysisResultsForElements(elements: Collection<JetElement>): AnalyzeExhaust {
        return getAnalysisResults(Task(elements.toSet()))
    }

    private fun getContainingNonlocalDeclaration(element: JetElement): JetElement? {
        return PsiTreeUtil.getParentOfType(element, javaClass<JetFile>(), false);
    }

    private fun handleError(e: Throwable) {
        DiagnosticUtils.throwIfRunningOnServer(e)
        LOG.error(e)
    }
}
