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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.analyzer.EmptyResolverForProject
import org.jetbrains.kotlin.asJava.outOfBlockModificationCount
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.project.AnalyzerFacadeProvider
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.diagnostics.KotlinSuppressCache
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.sumByLong

internal val LOG = Logger.getInstance(KotlinCacheService::class.java)

class KotlinCacheServiceImpl(val project: Project) : KotlinCacheService {
    override fun getResolutionFacade(elements: List<KtElement>): ResolutionFacade {
        return getFacadeToAnalyzeFiles(elements.map { it.getContainingKtFile() })
    }

    override fun getSuppressionCache(): KotlinSuppressCache = kotlinSuppressCache.value

    private val globalFacadesPerPlatformAndSdk: SLRUCache<Pair<TargetPlatform, Sdk?>, GlobalFacade> =
            object : SLRUCache<Pair<TargetPlatform, Sdk?>, GlobalFacade>(2 * 3, 2 * 3) {
                override fun createValue(key: Pair<TargetPlatform, Sdk?>): GlobalFacade {
                    return GlobalFacade(key.first, key.second)
                }
            }

    private val globalContext = GlobalContext(logProcessCanceled = true)

    // TODO: soft reference?
    // TODO: cache by script definition?
    private val facadeForScriptDependencies by lazy {
        ProjectResolutionFacade(project, globalContext.storageManager) {
            globalResolveSessionProvider(
                    "dependencies of scripts",
                    project,
                    JvmPlatform, // TODO: Js scripts?
                    null, // TODO: provide sdk via dependencies
                    commonGlobalContext = globalContext,
                    allModules = listOf(ScriptDependenciesModuleInfo(project)),
                    dependencies = listOf(
                            LibraryModificationTracker.getInstance(project), //TODO: provide correct trackers
                            ProjectRootModificationTracker.getInstance(project)
                    ),
                    moduleFilter = { true }
            )
        }
    }

    private inner class GlobalFacade(platform: TargetPlatform, sdk: Sdk?) {
        val facadeForLibraries = ProjectResolutionFacade(project, globalContext.storageManager) {
            globalResolveSessionProvider(
                    "project libraries for platform $platform",
                    project,
                    platform,
                    sdk,
                    commonGlobalContext = globalContext,
                    moduleFilter = { it.isLibraryClasses() },
                    dependencies = listOf(
                            LibraryModificationTracker.getInstance(project),
                            ProjectRootModificationTracker.getInstance(project)
                    )
            )
        }

        val facadeForModules = ProjectResolutionFacade(project, globalContext.storageManager) {
            globalResolveSessionProvider(
                    "project source roots and libraries for platform $platform",
                    project,
                    platform,
                    sdk,
                    commonGlobalContext = globalContext,
                    reuseDataFrom = facadeForLibraries,
                    moduleFilter = { !it.isLibraryClasses() },
                    dependencies = listOf(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT))
        }
    }

    @Deprecated("Use JetElement.getResolutionFacade(), please avoid introducing new usages")
    fun <T : Any> getProjectService(platform: TargetPlatform, ideaModuleInfo: IdeaModuleInfo, serviceClass: Class<T>): T {
        return globalFacade(platform, ideaModuleInfo.sdk).resolverForModuleInfo(ideaModuleInfo).componentProvider.getService(serviceClass)
    }

    private fun globalFacade(platform: TargetPlatform, sdk: Sdk?) =
            getOrBuildGlobalFacade(platform, sdk).facadeForModules

    private fun librariesFacade(platform: TargetPlatform, sdk: Sdk?) = getOrBuildGlobalFacade(platform, sdk).facadeForLibraries

    @Synchronized
    private fun getOrBuildGlobalFacade(platform: TargetPlatform, sdk: Sdk?) = globalFacadesPerPlatformAndSdk[Pair(platform, sdk)]

    private val IdeaModuleInfo.sdk: Sdk? get() = dependencies().firstIsInstanceOrNull<SdkInfo>()?.sdk

    private fun createFacadeForSyntheticFiles(files: Set<KtFile>): ProjectResolutionFacade {
        // we assume that all files come from the same module
        val targetPlatform = files.map { TargetPlatformDetector.getPlatform(it) }.toSet().single()
        val syntheticFileModule = files.map { it.getModuleInfo() }.toSet().single()
        val sdk = syntheticFileModule.sdk
        val filesModificationTracker = ModificationTracker {
            // TODO: Check getUserData(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT) actually works
            files.sumByLong { it.outOfBlockModificationCount + it.modificationStamp }
        }
        val dependenciesForSyntheticFileCache = listOf(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT, filesModificationTracker)
        val debugName = "completion/highlighting in $syntheticFileModule for files ${files.joinToString { it.name }} for platform $targetPlatform"

        fun makeGlobalResolveSessionProvider(reuseDataFrom: ProjectResolutionFacade? = null,
                                             moduleFilter: (IdeaModuleInfo) -> Boolean = { true },
                                             allModules: Collection<IdeaModuleInfo>? = null
        ): CachedValueProvider.Result<ModuleResolverProvider> {
            return globalResolveSessionProvider(
                    debugName,
                    project,
                    targetPlatform,
                    sdk,
                    commonGlobalContext = globalContext,
                    syntheticFiles = files,
                    reuseDataFrom = reuseDataFrom,
                    moduleFilter = moduleFilter,
                    dependencies = dependenciesForSyntheticFileCache,
                    allModules = allModules
            )
        }

        return when {
            syntheticFileModule is ModuleSourceInfo -> {
                val dependentModules = syntheticFileModule.getDependentModules()
                ProjectResolutionFacade(project, globalContext.storageManager) {
                    makeGlobalResolveSessionProvider(
                            reuseDataFrom = globalFacade(targetPlatform, sdk),
                            moduleFilter = { it in dependentModules })
                }
            }

            syntheticFileModule is ScriptModuleInfo || syntheticFileModule is ScriptDependenciesModuleInfo -> {
                ProjectResolutionFacade(project, globalContext.storageManager) {
                    makeGlobalResolveSessionProvider(
                            reuseDataFrom = facadeForScriptDependencies,
                            allModules = listOf(syntheticFileModule) + syntheticFileModule.dependencies(),
                            moduleFilter = { it == syntheticFileModule }
                    )
                }
            }

            syntheticFileModule is LibrarySourceInfo || syntheticFileModule is NotUnderContentRootModuleInfo -> {
                ProjectResolutionFacade(project, globalContext.storageManager) {
                    makeGlobalResolveSessionProvider(
                            reuseDataFrom = librariesFacade(targetPlatform, sdk),
                            moduleFilter = { it == syntheticFileModule }
                    )
                }
            }

            syntheticFileModule.isLibraryClasses() -> {
                //NOTE: this code should not be called for sdk or library classes
                // currently the only known scenario is when we cannot determine that file is a library source
                // (file under both classes and sources root)
                LOG.warn("Creating cache with synthetic files ($files) in classes of library $syntheticFileModule")
                ProjectResolutionFacade(project, globalContext.storageManager) {
                    makeGlobalResolveSessionProvider()
                }
            }

            else -> throw IllegalStateException("Unknown IdeaModuleInfo ${syntheticFileModule.javaClass}")
        }
    }

    private val suppressAnnotationShortName = KotlinBuiltIns.FQ_NAMES.suppress.shortName().identifier
    private val kotlinSuppressCache: CachedValue<KotlinSuppressCache> = CachedValuesManager.getManager(project).createCachedValue({
        CachedValueProvider.Result<KotlinSuppressCache>(object : KotlinSuppressCache() {
            override fun getSuppressionAnnotations(annotated: KtAnnotated): List<AnnotationDescriptor> {
                if (annotated.annotationEntries.none {
                        it.calleeExpression?.text?.endsWith(suppressAnnotationShortName) ?: false }) {
                    // Avoid running resolve heuristics
                    // TODO: Check aliases in imports
                    return emptyList()
                }

                val context = when (annotated) {
                    is KtFile -> annotated.fileAnnotationList?.analyze(BodyResolveMode.PARTIAL) ?: return emptyList()
                    is KtModifierListOwner -> annotated.modifierList?.analyze(BodyResolveMode.PARTIAL) ?: return emptyList()
                    else -> annotated.analyze(BodyResolveMode.PARTIAL)
                }

                val annotatedDescriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, annotated)

                if (annotatedDescriptor != null) {
                    return annotatedDescriptor.annotations.toList()
                }
                else {
                    return annotated.annotationEntries.mapNotNull { context.get(BindingContext.ANNOTATION, it) }
                }
            }
        }, LibraryModificationTracker.getInstance(project), PsiModificationTracker.MODIFICATION_COUNT)
    }, false)

    private val syntheticFileCachesLock = Any()

    private val syntheticFilesCacheProvider = CachedValueProvider {
        CachedValueProvider.Result(object : SLRUCache<Set<KtFile>, ProjectResolutionFacade>(2, 3) {
            override fun createValue(files: Set<KtFile>) = createFacadeForSyntheticFiles(files)
        }, LibraryModificationTracker.getInstance(project), ProjectRootModificationTracker.getInstance(project))
    }

    private fun getFacadeForSyntheticFiles(files: Set<KtFile>): ProjectResolutionFacade {
        synchronized(syntheticFileCachesLock) {
            //NOTE: computations inside createCacheForSyntheticFiles depend on project root structure
            // so we additionally drop the whole slru cache on change
            return CachedValuesManager.getManager(project).getCachedValue(project, syntheticFilesCacheProvider).get(files)
        }
    }

    private fun getFacadeToAnalyzeFiles(files: Collection<KtFile>): ResolutionFacade {
        val syntheticFiles = findSyntheticFiles(files)
        val file = files.first()
        val moduleInfo = file.getModuleInfo()
        val projectFacade = if (syntheticFiles.isNotEmpty()) {
            getFacadeForSyntheticFiles(syntheticFiles)
        }
        else {
            globalFacade(TargetPlatformDetector.getPlatform(file), moduleInfo.sdk)
        }
        return ResolutionFacadeImpl(projectFacade, moduleInfo)
    }

    private fun findSyntheticFiles(files: Collection<KtFile>) = files.mapNotNull {
        if (it is KtCodeFragment) it.getContextFile() else it
    }.filter {
        !ProjectRootsUtil.isInProjectSource(it)
    }.toSet()

    private fun KtCodeFragment.getContextFile(): KtFile? {
        val contextElement = context ?: return null
        val contextFile = (contextElement as? KtElement)?.getContainingKtFile()
                          ?: throw AssertionError("Analyzing kotlin code fragment of type $javaClass with java context of type ${contextElement.javaClass}")
        return if (contextFile is KtCodeFragment) contextFile.getContextFile() else contextFile
    }
}

private fun globalResolveSessionProvider(
        debugName: String,
        project: Project,
        platform: TargetPlatform,
        sdk: Sdk?,
        dependencies: Collection<Any>,
        moduleFilter: (IdeaModuleInfo) -> Boolean,
        commonGlobalContext: GlobalContextImpl,
        reuseDataFrom: ProjectResolutionFacade? = null,
        syntheticFiles: Collection<KtFile> = listOf(),
        allModules: Collection<IdeaModuleInfo>? = null // null means create resolvers for modules from idea model
): CachedValueProvider.Result<ModuleResolverProvider> {
    val delegateResolverProvider = reuseDataFrom?.moduleResolverProvider
    val delegateResolverForProject = delegateResolverProvider?.resolverForProject ?: EmptyResolverForProject()
    val globalContext = commonGlobalContext.withCompositeExceptionTrackerUnderSameLock()

    val builtIns: KotlinBuiltIns = when (platform) {
        is JsPlatform -> JsPlatform.builtIns
        is JvmPlatform -> JvmBuiltIns(globalContext.storageManager)
        else -> DefaultBuiltIns.Instance
    }

    val moduleResolverProvider = createModuleResolverProvider(
            debugName, project, globalContext, sdk,
            AnalyzerFacadeProvider.getAnalyzerFacade(platform),
            syntheticFiles, delegateResolverForProject, moduleFilter,
            allModules, builtIns
    )

    if (builtIns is JvmBuiltIns) {
        val moduleInfoToUse = sdk?.let { SdkInfo(project, it) } ?: moduleResolverProvider.resolverForProject.allModules.firstOrNull()
        if (moduleInfoToUse != null) {
            builtIns.setOwnerModuleDescriptor(moduleResolverProvider.resolverForProject.descriptorForModule(moduleInfoToUse))
        }
    }

    val allDependencies = dependencies + listOf(moduleResolverProvider.exceptionTracker)
    return CachedValueProvider.Result.create(moduleResolverProvider, allDependencies)
}
