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
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.project.AnalyzerFacadeProvider
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.project.outOfBlockModificationCount
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
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
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


    private val facadesForScriptDependencies: SLRUCache<ComparableScriptDependencies, ProjectResolutionFacade> =
            object : SLRUCache<ComparableScriptDependencies, ProjectResolutionFacade>(2, 3) {
                override fun createValue(key: ComparableScriptDependencies?): ProjectResolutionFacade {
                    return createFacadeForScriptDependencies(ScriptDependenciesModuleInfo(project, key))
                }
            }

    private fun getFacadeForScriptDependencies(dependencies: ComparableScriptDependencies?) = synchronized(facadesForScriptDependencies) {
        facadesForScriptDependencies.get(dependencies)
    }

    private fun createFacadeForScriptDependencies(
            dependenciesModuleInfo: ScriptDependenciesModuleInfo,
            syntheticFiles: Collection<KtFile> = listOf()
    ): ProjectResolutionFacade {
        val sdk = findJdk(dependenciesModuleInfo.dependencies, project)
        val platform = JvmPlatform // TODO: Js scripts?
        val sdkFacade = GlobalFacade(platform, sdk).facadeForSdk
        val globalContext = sdkFacade.globalContext.contextWithNewLockAndCompositeExceptionTracker()
        return ProjectResolutionFacade(
                "facadeForScriptDependencies",
                project, globalContext,
                globalResolveSessionProvider(
                        "dependencies of scripts",
                        platform,
                        sdk,
                        reuseDataFrom = sdkFacade,
                        allModules = dependenciesModuleInfo.dependencies(),
                        //TODO: provide correct trackers
                        dependencies = listOf(
                                LibraryModificationTracker.getInstance(project),
                                ProjectRootModificationTracker.getInstance(project)
                        ),
                        moduleFilter = { it == dependenciesModuleInfo },
                        syntheticFiles = syntheticFiles
                )
        )
    }


    private inner class GlobalFacade(platform: TargetPlatform, sdk: Sdk?) {
        private val sdkContext = GlobalContext()
        val facadeForSdk = ProjectResolutionFacade(
                "facadeForSdk",
                project, sdkContext,
                globalResolveSessionProvider(
                        "sdk $sdk",
                        platform,
                        sdk,
                        allModules = sdk?.let { SdkInfo(project, it) }.singletonOrEmptyList(),
                        moduleFilter = { it is SdkInfo },
                        dependencies = listOf(
                                LibraryModificationTracker.getInstance(project),
                                ProjectRootModificationTracker.getInstance(project)
                        )
                ))

        private val librariesContext = sdkContext.contextWithNewLockAndCompositeExceptionTracker()
        val facadeForLibraries = ProjectResolutionFacade(
                "facadeForLibraries",
                project, librariesContext,
                globalResolveSessionProvider(
                        "project libraries for platform $platform",
                        platform,
                        sdk,
                        reuseDataFrom = facadeForSdk,
                        moduleFilter = { it is LibraryInfo },
                        dependencies = listOf(
                                LibraryModificationTracker.getInstance(project),
                                ProjectRootModificationTracker.getInstance(project)
                        )
                )
        )


        private val modulesContext = librariesContext.contextWithNewLockAndCompositeExceptionTracker()
        val facadeForModules = ProjectResolutionFacade(
                "facadeForModules",
                project, modulesContext,
                globalResolveSessionProvider(
                        "project source roots and libraries for platform $platform",
                        platform,
                        sdk,
                        reuseDataFrom = facadeForLibraries,
                        moduleFilter = { !it.isLibraryClasses() },
                        dependencies = listOf(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT))
        )
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
        ): (GlobalContextImpl, Project) -> ModuleResolverProvider {
            return globalResolveSessionProvider(
                    debugName,
                    targetPlatform,
                    sdk,
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
                val modulesFacade = globalFacade(targetPlatform, sdk)
                val globalContext = modulesFacade.globalContext.contextWithNewLockAndCompositeExceptionTracker()
                ProjectResolutionFacade(
                        "facadeForSynthetic in ModuleSourceInfo",
                        project, globalContext,
                        makeGlobalResolveSessionProvider(
                                reuseDataFrom = modulesFacade,
                                moduleFilter = { it in dependentModules })
                )
            }

            syntheticFileModule is ScriptDependenciesModuleInfo -> {
                createFacadeForScriptDependencies(syntheticFileModule, files)
            }
            syntheticFileModule is ScriptModuleInfo -> {
                val facadeForScriptDependencies = getFacadeForScriptDependencies(syntheticFileModule.externalDependencies)
                val globalContext = facadeForScriptDependencies.globalContext.contextWithNewLockAndCompositeExceptionTracker()
                ProjectResolutionFacade(
                        "facadeForSynthetic in ScriptModuleInfo",
                        project, globalContext,
                        makeGlobalResolveSessionProvider(
                                reuseDataFrom = facadeForScriptDependencies,
                                allModules = syntheticFileModule.dependencies(),
                                moduleFilter = { it == syntheticFileModule }
                        )
                )
            }

            syntheticFileModule is LibrarySourceInfo || syntheticFileModule is NotUnderContentRootModuleInfo -> {
                val librariesFacade = librariesFacade(targetPlatform, sdk)
                val globalContext = librariesFacade.globalContext.contextWithNewLockAndCompositeExceptionTracker()
                ProjectResolutionFacade(
                        "facadeForSynthetic in LibrarySourceInfo or NotUnderContentRootModuleInfo",
                        project, globalContext,
                        makeGlobalResolveSessionProvider(
                                reuseDataFrom = librariesFacade,
                                moduleFilter = { it == syntheticFileModule }
                        )
                )
            }

            syntheticFileModule.isLibraryClasses() -> {
                //NOTE: this code should not be called for sdk or library classes
                // currently the only known scenario is when we cannot determine that file is a library source
                // (file under both classes and sources root)
                LOG.warn("Creating cache with synthetic files ($files) in classes of library $syntheticFileModule")
                val globalContext = GlobalContext()
                ProjectResolutionFacade(
                        "facadeForSynthetic for file under both classes and root",
                        project, globalContext,
                        makeGlobalResolveSessionProvider()
                )
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
        platform: TargetPlatform,
        sdk: Sdk?,
        dependencies: Collection<Any>,
        moduleFilter: (IdeaModuleInfo) -> Boolean,
        reuseDataFrom: ProjectResolutionFacade? = null,
        syntheticFiles: Collection<KtFile> = listOf(),
        allModules: Collection<IdeaModuleInfo>? = null // null means create resolvers for modules from idea model
): (GlobalContextImpl, Project) -> ModuleResolverProvider = { globalContext, project ->
    val delegateResolverProvider = reuseDataFrom?.moduleResolverProvider
    val delegateResolverForProject = delegateResolverProvider?.resolverForProject ?: EmptyResolverForProject()

    val reusedBuiltIns = delegateResolverProvider?.builtIns
    val newBuiltIns: KotlinBuiltIns? = when {
        reusedBuiltIns != null -> null
        platform is JsPlatform -> JsPlatform.builtIns
        platform is JvmPlatform && sdk != null -> JvmBuiltIns(globalContext.storageManager)
        else -> DefaultBuiltIns.Instance
    }

    val builtIns = reusedBuiltIns ?: newBuiltIns!!
    val moduleResolverProvider = createModuleResolverProvider(
            debugName, project, globalContext, sdk,
            AnalyzerFacadeProvider.getAnalyzerFacade(platform),
            syntheticFiles, delegateResolverForProject, moduleFilter,
            allModules,
            builtIns,
            dependencies
    )

    if (newBuiltIns is JvmBuiltIns) {
        val sdkInfo = SdkInfo(project, sdk!!)
        newBuiltIns.setOwnerModuleDescriptor(moduleResolverProvider.resolverForProject.descriptorForModule(sdkInfo))
    }

    moduleResolverProvider
}

