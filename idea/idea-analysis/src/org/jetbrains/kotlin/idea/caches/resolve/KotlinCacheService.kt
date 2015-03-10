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
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.SLRUCache
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.kotlin.resolve.BindingContext
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.idea.project.AnalyzerFacadeProvider
import org.jetbrains.kotlin.idea.project.TargetPlatform
import org.jetbrains.kotlin.idea.project.TargetPlatform.*
import org.jetbrains.kotlin.idea.project.ResolveSessionForBodies
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.utils.keysToMap
import com.intellij.openapi.roots.ProjectRootModificationTracker
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.JetScope
import kotlin.platform.platformStatic

private val LOG = Logger.getInstance(javaClass<KotlinCacheService>())

public class KotlinCacheService(val project: Project) {
    default object {
        platformStatic public fun getInstance(project: Project): KotlinCacheService = ServiceManager.getService(project, javaClass<KotlinCacheService>())!!
    }

    public fun getResolutionFacade(elements: List<JetElement>): ResolutionFacade {
        val cache = getCacheToAnalyzeFiles(elements.map { it.getContainingJetFile() })
        return object : ResolutionFacade {
            override fun analyze(element: JetElement, bodyResolveMode: BodyResolveMode): BindingContext {
                return cache.getLazyResolveSession(element).resolveToElement(element, bodyResolveMode)
            }

            override fun findModuleDescriptor(element: JetElement): ModuleDescriptor {
                return cache.getLazyResolveSession(element).getModuleDescriptor()
            }

            override fun resolveToDescriptor(declaration: JetDeclaration): DeclarationDescriptor {
                return cache.getLazyResolveSession(declaration).resolveToDescriptor(declaration)
            }

            override fun analyzeFullyAndGetResult(elements: Collection<JetElement>): AnalysisResult {
                return cache.getAnalysisResultsForElements(elements)
            }

            override fun getFileTopLevelScope(file: JetFile): JetScope {
                return cache.getLazyResolveSession(file).getScopeProvider().getFileScope(file)
            }

            override fun <T> get(extension: CacheExtension<T>): T {
                return cache[extension]
            }
        }
    }

    fun globalResolveSessionProvider(
            platform: TargetPlatform,
            dependencies: Collection<Any>,
            moduleFilter: (IdeaModuleInfo) -> Boolean,
            reuseDataFromCache: KotlinResolveCache? = null,
            syntheticFiles: Collection<JetFile> = listOf(),
            logProcessCanceled: Boolean = false
    ): () -> CachedValueProvider.Result<ModuleResolverProvider> = {
        val analyzerFacade = AnalyzerFacadeProvider.getAnalyzerFacade(platform)
        val delegateResolverProvider = reuseDataFromCache?.moduleResolverProvider ?: EmptyModuleResolverProvider
        val globalContext = (delegateResolverProvider as? ModuleResolverProviderImpl)?.globalContext
                                    ?.withCompositeExceptionTrackerUnderSameLock()
                            ?: GlobalContext(logProcessCanceled)

        val moduleResolverProvider = createModuleResolverProvider(
                project, globalContext, analyzerFacade, syntheticFiles, delegateResolverProvider, moduleFilter
        )
        val allDependencies = dependencies + listOf(moduleResolverProvider.exceptionTracker)
        CachedValueProvider.Result.create(moduleResolverProvider, allDependencies)
    }

    private val globalCachesPerPlatform = listOf(JVM, JS).keysToMap { platform -> GlobalCache(platform) }

    private inner class GlobalCache(platform: TargetPlatform) {
        val librariesCache = KotlinResolveCache(
                project, globalResolveSessionProvider(platform,
                                                      logProcessCanceled = true,
                                                      moduleFilter = { it.isLibraryClasses() },
                                                      dependencies = listOf(
                                                              LibraryModificationTracker.getInstance(project),
                                                              ProjectRootModificationTracker.getInstance(project)))
        )

        val modulesCache = KotlinResolveCache(
                project, globalResolveSessionProvider(platform,
                                                      reuseDataFromCache = librariesCache,
                                                      moduleFilter = { !it.isLibraryClasses() },
                                                      dependencies = listOf(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT))
        )
    }

    private fun getGlobalCache(platform: TargetPlatform) = globalCachesPerPlatform[platform]!!.modulesCache
    private fun getGlobalLibrariesCache(platform: TargetPlatform) = globalCachesPerPlatform[platform]!!.librariesCache

    private val syntheticFileCaches = object : SLRUCache<Set<JetFile>, KotlinResolveCache>(2, 3) {
        override fun createValue(files: Set<JetFile>): KotlinResolveCache {
            // we assume that all files come from the same module
            val targetPlatform = files.map { TargetPlatformDetector.getPlatform(it) }.toSet().single()
            val syntheticFileModule = files.map { it.getModuleInfo() }.toSet().single()
            return when {
                syntheticFileModule is ModuleSourceInfo -> {
                    val dependentModules = syntheticFileModule.getDependentModules()
                    KotlinResolveCache(
                            project,
                            globalResolveSessionProvider(
                                    targetPlatform,
                                    syntheticFiles = files,
                                    reuseDataFromCache = getGlobalCache(targetPlatform),
                                    moduleFilter = { it in dependentModules },
                                    dependencies = listOf(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
                            )
                    )
                }

                syntheticFileModule is LibrarySourceInfo || syntheticFileModule is NotUnderContentRootModuleInfo -> {
                    KotlinResolveCache(
                            project,
                            globalResolveSessionProvider(
                                    targetPlatform,
                                    syntheticFiles = files,
                                    reuseDataFromCache = getGlobalLibrariesCache(targetPlatform),
                                    moduleFilter = { it == syntheticFileModule },
                                    dependencies = listOf(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
                            )
                    )
                }

                syntheticFileModule.isLibraryClasses() -> {
                    //NOTE: this code should not be called for sdk or library classes
                    // currently the only known scenario is when we cannot determine that file is a library source
                    // (file under both classes and sources root)
                    LOG.warn("Creating cache with synthetic files ($files) in classes of library $syntheticFileModule")
                    KotlinResolveCache(
                            project,
                            globalResolveSessionProvider(
                                    targetPlatform,
                                    syntheticFiles = files,
                                    moduleFilter = { true },
                                    dependencies = listOf(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
                            )
                    )
                }

                else -> throw IllegalStateException("Unknown IdeaModuleInfo ${syntheticFileModule.javaClass}")
            }
        }
    }

    private fun getCacheForSyntheticFiles(files: Set<JetFile>): KotlinResolveCache {
        return synchronized(syntheticFileCaches) {
            syntheticFileCaches[files]
        }
    }

    public fun getLazyResolveSession(element: JetElement): ResolveSessionForBodies {
        val file = element.getContainingJetFile()
        return getCacheToAnalyzeFiles(listOf(file)).getLazyResolveSession(file)
    }

    public fun getAnalysisResults(elements: Collection<JetElement>): AnalysisResult {
        if (elements.isEmpty()) return AnalysisResult.EMPTY

        val files = elements.map { it.getContainingJetFile() }.toSet()
        assertAreInSameModule(files)

        return getCacheToAnalyzeFiles(files).getAnalysisResultsForElements(elements)
    }

    private fun getCacheToAnalyzeFiles(files: Collection<JetFile>): KotlinResolveCache {
        val syntheticFiles = findSyntheticFiles(files)
        return if (syntheticFiles.isNotEmpty()) {
            getCacheForSyntheticFiles(syntheticFiles)
        }
        else {
            getGlobalCache(TargetPlatformDetector.getPlatform(files.first()))
        }
    }

    private fun findSyntheticFiles(files: Collection<JetFile>) = files.map {
        if (it is JetCodeFragment) it.getContextFile() else it
    }.filter {
        !ProjectRootsUtil.isInProjectSource(it)
    }.toSet()

    private fun JetCodeFragment.getContextFile(): JetFile {
        val contextElement = getContext() ?: throw AssertionError("Analyzing code fragment of type $javaClass with no context")
        val contextFile = (contextElement as? JetElement)?.getContainingJetFile()
                          ?: throw AssertionError("Analyzing kotlin code fragment of type $javaClass with java context of type ${contextElement.javaClass}")
        return if (contextFile is JetCodeFragment) contextFile.getContextFile() else contextFile
    }

    private fun assertAreInSameModule(elements: Collection<JetElement>) {
        if (elements.size() <= 1) {
            return
        }
        val thisInfo = elements.first().getModuleInfo()
        elements.forEach {
            val extraFileInfo = it.getModuleInfo()
            assert(extraFileInfo == thisInfo, "All files under analysis should be in the same module.\nExpected: $thisInfo\nWas:${extraFileInfo}")
        }
    }

    public fun <T> get(extension: CacheExtension<T>): T {
        return getGlobalCache(extension.platform)[extension]
    }
}
