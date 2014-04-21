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

import org.jetbrains.jet.lang.resolve.lazy.ResolveSession
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.project.Project
import org.jetbrains.jet.analyzer.AnalyzeExhaust
import com.intellij.psi.util.CachedValuesManager
import com.intellij.openapi.diagnostic.Logger
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

private val LOG = Logger.getInstance(javaClass<KotlinResolveCache>())

class KotlinResolveCache(
        val project: Project,
        val resolveSession: ResolveSession
) {

    private data class Task(
            val elements: Set<JetElement>
    )

    private val analysisResults = CachedValuesManager.getManager(project).createCachedValue ({
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
                    val resultingContext = injector.getLazyTopDownAnalyzer()!!.analyzeDeclarations(
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
                            resultingContext,
                            resolveSession.getModuleDescriptor()
                    )
                }
                catch (e: ProcessCanceledException) {
                    throw e
                }
                catch (e: Throwable) {
                    handleError(e)

                    // Exception during body resolve analyze can harm internal caches in declarations cache
                    KotlinCacheManager.getInstance(project).invalidateCache()

                    val bindingTraceContext = BindingTraceContext()
                    return AnalyzeExhaust.error(bindingTraceContext.getBindingContext(), e)
                }
            }
        }

        CachedValueProvider.Result(results, PsiModificationTracker.MODIFICATION_COUNT, resolveSession.getExceptionTracker())
    }, false)

    private fun getAnalysisResults(task: Task): AnalyzeExhaust {
        return synchronized(analysisResults) {
            analysisResults.getValue()!![task]
        }
    }

    fun getAnalysisResultsForElement(element: JetElement): AnalyzeExhaust {
        return getAnalysisResults(Task(setOf(element)))
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
