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
import com.intellij.openapi.project.Project
import org.jetbrains.jet.analyzer.AnalyzeExhaust
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.SLRUCache
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.jet.lang.resolve.BindingContext
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.plugin.project.AnalyzerFacadeProvider
import org.jetbrains.jet.plugin.project.TargetPlatform
import org.jetbrains.jet.plugin.project.TargetPlatform.*
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.plugin.project.TargetPlatformDetector
import org.jetbrains.jet.lang.psi.JetCodeFragment
import org.jetbrains.jet.plugin.stubindex.JetSourceFilterScope

private val LOG = Logger.getInstance(javaClass<KotlinCacheService>())

fun JetElement.getLazyResolveSession(): ResolveSessionForBodies {
    return KotlinCacheService.getInstance(getProject()).getLazyResolveSession(this)
}

fun Project.getLazyResolveSession(platform: TargetPlatform): ResolveSessionForBodies {
    return KotlinCacheService.getInstance(this).getGlobalLazyResolveSession(platform)
}

fun JetElement.getAnalysisResults(): AnalyzeExhaust {
    return KotlinCacheService.getInstance(getProject()).getAnalysisResults(listOf(this))
}

fun JetElement.getBindingContext(): BindingContext {
    return getAnalysisResults().getBindingContext()
}

fun getAnalysisResultsForElements(elements: Collection<JetElement>): AnalyzeExhaust {
    if (elements.isEmpty()) return AnalyzeExhaust.EMPTY
    val element = elements.first()
    return KotlinCacheService.getInstance(element.getProject()).getAnalysisResults(elements)
}

class KotlinCacheService(val project: Project) {
    class object {
        fun getInstance(project: Project) = ServiceManager.getService(project, javaClass<KotlinCacheService>())!!
    }

    private fun globalResolveSessionProvider(platform: TargetPlatform, syntheticFiles: Collection<JetFile> = listOf()) = {
        val setup = AnalyzerFacadeProvider.getAnalyzerFacade(platform)
                .createSetup(project, syntheticFiles, GlobalSearchScope.allScope(project))
        val resolveSessionForBodies = ResolveSessionForBodies(project, setup.getLazyResolveSession())
        CachedValueProvider.Result.create(
                SessionAndSetup(
                        platform,
                        resolveSessionForBodies,
                        setup
                ),
                PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
                resolveSessionForBodies
        )
    }

    private val globalCachesPerPlatform = mapOf(
            JVM to KotlinResolveCache(project, globalResolveSessionProvider(JVM)),
            JS to KotlinResolveCache(project, globalResolveSessionProvider(JS))
    )

    private val syntheticFileCaches = object : SLRUCache<JetFile, KotlinResolveCache>(2, 3) {
        override fun createValue(file: JetFile?): KotlinResolveCache {
            return KotlinResolveCache(
                    project,
                    globalResolveSessionProvider(
                            TargetPlatformDetector.getPlatform(file!!),
                            listOf(file)
                    )
            )
        }
    }

    private fun getCacheForSyntheticFile(file: JetFile): KotlinResolveCache {
        return synchronized(syntheticFileCaches) {
            syntheticFileCaches[file]
        }
    }

    public fun getGlobalLazyResolveSession(platform: TargetPlatform): ResolveSessionForBodies {
        return globalCachesPerPlatform[platform]!!.getLazyResolveSession()
    }

    public fun getLazyResolveSession(element: JetElement): ResolveSessionForBodies {
        val file = element.getContainingJetFile()
        if (!isFileInScope(file)) {
            return getCacheForSyntheticFile(file).getLazyResolveSession()
        }

        return getGlobalLazyResolveSession(TargetPlatformDetector.getPlatform(file))
    }

    public fun getAnalysisResults(elements: Collection<JetElement>): AnalyzeExhaust {
        if (elements.isEmpty()) return AnalyzeExhaust.EMPTY

        val firstFile = elements.first().getContainingJetFile()
        if (elements.size == 1 && (!isFileInScope(firstFile) && firstFile !is JetCodeFragment)) {
            return getCacheForSyntheticFile(firstFile).getAnalysisResultsForElements(elements)
        }

        val resolveCache = globalCachesPerPlatform[TargetPlatformDetector.getPlatform(firstFile)]!!
        return resolveCache.getAnalysisResultsForElements(elements)
    }

    private fun isFileInScope(jetFile: JetFile): Boolean {
        val virtualFile = jetFile.getVirtualFile()
        if (virtualFile == null) {
            return false
        }
        return virtualFile in kotlinSourcesInProjectScope()
    }

    private fun kotlinSourcesInProjectScope(): GlobalSearchScope {
        return JetSourceFilterScope.kotlinSources(GlobalSearchScope.allScope(project))
    }

    public fun <T> get(extension: CacheExtension<T>): T {
        return globalCachesPerPlatform[extension.platform]!![extension]
    }
}