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
import com.intellij.psi.PsiCodeFragment
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.SLRUCache
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ResolverForProject.Companion.resolverForLibrariesName
import org.jetbrains.kotlin.analyzer.ResolverForProject.Companion.resolverForModulesName
import org.jetbrains.kotlin.analyzer.ResolverForProject.Companion.resolverForScriptDependenciesName
import org.jetbrains.kotlin.analyzer.ResolverForProject.Companion.resolverForSdkName
import org.jetbrains.kotlin.analyzer.ResolverForProject.Companion.resolverForSpecialInfoName
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.context.GlobalContextImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.resolve.util.contextWithNewLockAndCompositeExceptionTracker
import org.jetbrains.kotlin.idea.compiler.IDELanguageSettingsProvider
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.project.outOfBlockModificationCount
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.diagnostics.KotlinSuppressCache
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.sumByLong
import java.lang.AssertionError
import java.lang.IllegalStateException

internal val LOG = Logger.getInstance(KotlinCacheService::class.java)

// For every different instance of these settings we must create a different builtIns instance and thus a different moduleDescriptor graph
// since in the current implementation types from one module are leaking into other modules' resolution
// meaning that we can't just change those setting on a per module basis
data class PlatformAnalysisSettings(
    val platform: TargetPlatform, val sdk: Sdk?,
    val isAdditionalBuiltInFeaturesSupported: Boolean,
    // Effectively unused as a property. Needed only to distinguish different modes when being put in a map
    val isReleaseCoroutines: Boolean
)

class KotlinCacheServiceImpl(val project: Project) : KotlinCacheService {
    override fun getResolutionFacade(elements: List<KtElement>): ResolutionFacade {
        return getFacadeToAnalyzeFiles(elements.map {
            // in theory `containingKtFile` is `@NotNull` but in practice EA-114080
            @Suppress("USELESS_ELVIS")
            it.containingKtFile ?: throw IllegalStateException("containingKtFile was null for $it of ${it.javaClass}")
        })
    }

    override fun getSuppressionCache(): KotlinSuppressCache = kotlinSuppressCache.value

    private val globalFacadesPerPlatformAndSdk: SLRUCache<PlatformAnalysisSettings, GlobalFacade> =
        object : SLRUCache<PlatformAnalysisSettings, GlobalFacade>(2 * 3 * 2, 2 * 3 * 2) {
            override fun createValue(settings: PlatformAnalysisSettings): GlobalFacade {
                return GlobalFacade(settings)
            }
        }

    private val facadeForScriptDependenciesForProject = createFacadeForScriptDependencies(ScriptDependenciesInfo.ForProject(project))

    private fun createFacadeForScriptDependencies(
        dependenciesModuleInfo: ScriptDependenciesInfo,
        syntheticFiles: Collection<KtFile> = listOf()
    ): ProjectResolutionFacade {
        val sdk = dependenciesModuleInfo.sdk
        val platform = JvmPlatform // TODO: Js scripts?
        val settings = PlatformAnalysisSettings(
            platform, sdk, true,
            LanguageFeature.ReleaseCoroutines.defaultState == LanguageFeature.State.ENABLED
        )

        val dependenciesForScriptDependencies = listOf(
            LibraryModificationTracker.getInstance(project),
            ProjectRootModificationTracker.getInstance(project),
            ScriptDependenciesModificationTracker.getInstance(project)
        )

        val scriptFile = (dependenciesModuleInfo as? ScriptDependenciesInfo.ForFile)?.scriptFile
        val relatedModuleSourceInfo = scriptFile?.let { getScriptRelatedModuleInfo(project, it) }
        val globalFacade = if (relatedModuleSourceInfo != null) {
            globalFacade(settings)
        } else {
            getOrBuildGlobalFacade(settings).facadeForSdk
        }

        val globalContext = globalFacade.globalContext.contextWithNewLockAndCompositeExceptionTracker()
        return ProjectResolutionFacade(
            "facadeForScriptDependencies",
            resolverForScriptDependenciesName,
            project, globalContext, settings,
            reuseDataFrom = globalFacade,
            allModules = dependenciesModuleInfo.dependencies(),
            //TODO: provide correct trackers
            dependencies = dependenciesForScriptDependencies,
            moduleFilter = { it == dependenciesModuleInfo },
            invalidateOnOOCB = true,
            syntheticFiles = syntheticFiles
        )
    }


    private inner class GlobalFacade(settings: PlatformAnalysisSettings) {
        private val sdkContext = GlobalContext()
        val facadeForSdk = ProjectResolutionFacade(
            "facadeForSdk", "$resolverForSdkName ${settings.sdk}",
            project, sdkContext, settings,
            moduleFilter = { it is SdkInfo },
            dependencies = listOf(
                LibraryModificationTracker.getInstance(project),
                ProjectRootModificationTracker.getInstance(project)
            ),
            invalidateOnOOCB = false,
            reuseDataFrom = null
        )

        private val librariesContext = sdkContext.contextWithNewLockAndCompositeExceptionTracker()
        val facadeForLibraries = ProjectResolutionFacade(
            "facadeForLibraries", "$resolverForLibrariesName for platform ${settings.sdk}",
            project, librariesContext, settings,
            reuseDataFrom = facadeForSdk,
            moduleFilter = { it is LibraryInfo },
            invalidateOnOOCB = false,
            dependencies = listOf(
                LibraryModificationTracker.getInstance(project),
                ProjectRootModificationTracker.getInstance(project)
            )
        )

        private val modulesContext = librariesContext.contextWithNewLockAndCompositeExceptionTracker()
        val facadeForModules = ProjectResolutionFacade(
            "facadeForModules", "$resolverForModulesName for platform ${settings.platform}",
            project, modulesContext, settings,
            reuseDataFrom = facadeForLibraries,
            moduleFilter = { !it.isLibraryClasses() },
            dependencies = listOf(
                LibraryModificationTracker.getInstance(project),
                ProjectRootModificationTracker.getInstance(project)
            ),
            invalidateOnOOCB = true
        )
    }

    private fun IdeaModuleInfo.platformSettings(targetPlatform: TargetPlatform) = PlatformAnalysisSettings(
        targetPlatform, sdk,
        supportsAdditionalBuiltInsMembers(),
        isReleaseCoroutines()
    )

    private fun IdeaModuleInfo.supportsAdditionalBuiltInsMembers(): Boolean {
        return IDELanguageSettingsProvider
            .getLanguageVersionSettings(this, project)
            .supportsFeature(LanguageFeature.AdditionalBuiltInsMembers)
    }

    private fun IdeaModuleInfo.isReleaseCoroutines(): Boolean {
        return IDELanguageSettingsProvider
            .getLanguageVersionSettings(this, project)
            .supportsFeature(LanguageFeature.ReleaseCoroutines)
    }

    private fun globalFacade(settings: PlatformAnalysisSettings) =
        getOrBuildGlobalFacade(settings).facadeForModules

    private fun librariesFacade(settings: PlatformAnalysisSettings) =
        getOrBuildGlobalFacade(settings).facadeForLibraries

    @Synchronized
    private fun getOrBuildGlobalFacade(settings: PlatformAnalysisSettings) =
        globalFacadesPerPlatformAndSdk[settings]

    private val IdeaModuleInfo.sdk: Sdk? get() = dependencies().firstIsInstanceOrNull<SdkInfo>()?.sdk

    private fun createFacadeForFilesWithSpecialModuleInfo(files: Set<KtFile>): ProjectResolutionFacade {
        // we assume that all files come from the same module
        val targetPlatform = files.map { TargetPlatformDetector.getPlatform(it) }.toSet().single()
        val specialModuleInfo = files.map(KtFile::getModuleInfo).toSet().single()
        val settings = specialModuleInfo.platformSettings(targetPlatform)

        // File copies are created during completion and receive correct modification events through POM.
        // Dummy files created e.g. by J2K do not receive events.
        val filesModificationTracker = if (files.all { it.originalFile != it }) {
            ModificationTracker {
                files.sumByLong { it.outOfBlockModificationCount }
            }
        } else {
            ModificationTracker {
                files.sumByLong { it.outOfBlockModificationCount + it.modificationStamp }
            }
        }

        val dependenciesForSyntheticFileCache =
            listOf(
                PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT,
                filesModificationTracker
            )

        val resolverDebugName =
            "$resolverForSpecialInfoName $specialModuleInfo for files ${files.joinToString { it.name }} for platform $targetPlatform"

        fun makeProjectResolutionFacade(
            debugName: String,
            globalContext: GlobalContextImpl,
            reuseDataFrom: ProjectResolutionFacade? = null,
            moduleFilter: (IdeaModuleInfo) -> Boolean = { true },
            allModules: Collection<IdeaModuleInfo>? = null
        ): ProjectResolutionFacade {
            return ProjectResolutionFacade(
                debugName,
                resolverDebugName,
                project,
                globalContext,
                settings,
                syntheticFiles = files,
                reuseDataFrom = reuseDataFrom,
                moduleFilter = moduleFilter,
                dependencies = dependenciesForSyntheticFileCache,
                invalidateOnOOCB = true,
                allModules = allModules
            )
        }

        return when {
            specialModuleInfo is ModuleSourceInfo -> {
                val dependentModules = specialModuleInfo.getDependentModules()
                val modulesFacade = globalFacade(settings)
                val globalContext = modulesFacade.globalContext.contextWithNewLockAndCompositeExceptionTracker()
                makeProjectResolutionFacade(
                    "facadeForSpecialModuleInfo (ModuleSourceInfo)",
                    globalContext,
                    reuseDataFrom = modulesFacade,
                    moduleFilter = { it in dependentModules }
                )
            }

            specialModuleInfo is ScriptModuleInfo -> {
                val facadeForScriptDependencies = createFacadeForScriptDependencies(
                    ScriptDependenciesInfo.ForFile(project, specialModuleInfo.scriptFile, specialModuleInfo.scriptDefinition)
                )
                val globalContext = facadeForScriptDependencies.globalContext.contextWithNewLockAndCompositeExceptionTracker()
                makeProjectResolutionFacade(
                    "facadeForSpecialModuleInfo (ScriptModuleInfo)",
                    globalContext,
                    reuseDataFrom = facadeForScriptDependencies,
                    allModules = specialModuleInfo.dependencies(),
                    moduleFilter = { it == specialModuleInfo }
                )
            }
            specialModuleInfo is ScriptDependenciesInfo -> facadeForScriptDependenciesForProject
            specialModuleInfo is ScriptDependenciesSourceInfo -> {
                val globalContext = facadeForScriptDependenciesForProject.globalContext.contextWithNewLockAndCompositeExceptionTracker()
                makeProjectResolutionFacade(
                    "facadeForSpecialModuleInfo (ScriptDependenciesSourceInfo)",
                    globalContext,
                    reuseDataFrom = facadeForScriptDependenciesForProject,
                    allModules = specialModuleInfo.dependencies(),
                    moduleFilter = { it == specialModuleInfo }
                )
            }

            specialModuleInfo is LibrarySourceInfo || specialModuleInfo === NotUnderContentRootModuleInfo -> {
                val librariesFacade = librariesFacade(settings)
                val globalContext = librariesFacade.globalContext.contextWithNewLockAndCompositeExceptionTracker()
                makeProjectResolutionFacade(
                    "facadeForSpecialModuleInfo (LibrarySourceInfo or NotUnderContentRootModuleInfo)",
                    globalContext,
                    reuseDataFrom = librariesFacade,
                    moduleFilter = { it == specialModuleInfo }
                )
            }

            specialModuleInfo.isLibraryClasses() -> {
                //NOTE: this code should not be called for sdk or library classes
                // currently the only known scenario is when we cannot determine that file is a library source
                // (file under both classes and sources root)
                LOG.warn("Creating cache with synthetic files ($files) in classes of library $specialModuleInfo")
                val globalContext = GlobalContext()
                makeProjectResolutionFacade(
                    "facadeForSpecialModuleInfo for file under both classes and root",
                    globalContext
                )
            }

            else -> throw IllegalStateException("Unknown IdeaModuleInfo ${specialModuleInfo::class.java}")
        }
    }

    private val kotlinSuppressCache: CachedValue<KotlinSuppressCache> = CachedValuesManager.getManager(project).createCachedValue(
        {
            CachedValueProvider.Result<KotlinSuppressCache>(
                object : KotlinSuppressCache() {
                    override fun getSuppressionAnnotations(annotated: KtAnnotated): List<AnnotationDescriptor> {
                        if (annotated.annotationEntries.none {
                                it.calleeExpression?.text?.endsWith(SUPPRESS_ANNOTATION_SHORT_NAME) == true
                            }
                        ) {
                            // Avoid running resolve heuristics
                            // TODO: Check aliases in imports
                            return emptyList()
                        }

                        val context =
                            when (annotated) {
                                is KtFile -> {
                                    annotated.fileAnnotationList?.analyze(BodyResolveMode.PARTIAL)
                                            ?: return emptyList()
                                }
                                is KtModifierListOwner -> {
                                    annotated.modifierList?.analyze(BodyResolveMode.PARTIAL)
                                            ?: return emptyList()
                                }
                                else ->
                                    annotated.analyze(BodyResolveMode.PARTIAL)
                            }

                        val annotatedDescriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, annotated)

                        if (annotatedDescriptor != null) {
                            return annotatedDescriptor.annotations.toList()
                        }

                        return annotated.annotationEntries.mapNotNull {
                            context.get(
                                BindingContext.ANNOTATION,
                                it
                            )
                        }
                    }
                },
                LibraryModificationTracker.getInstance(project),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        },
        false
    )

    private val specialFilesCacheProvider = CachedValueProvider {
        // NOTE: computations inside createFacadeForFilesWithSpecialModuleInfo depend on project root structure
        // so we additionally drop the whole slru cache on change
        CachedValueProvider.Result(
            object : SLRUCache<Set<KtFile>, ProjectResolutionFacade>(2, 3) {
                override fun createValue(files: Set<KtFile>) = createFacadeForFilesWithSpecialModuleInfo(files)
            },
            LibraryModificationTracker.getInstance(project),
            ProjectRootModificationTracker.getInstance(project)
        )
    }

    private fun getFacadeForSpecialFiles(files: Set<KtFile>): ProjectResolutionFacade {
        val cachedValue: SLRUCache<Set<KtFile>, ProjectResolutionFacade> =
            CachedValuesManager.getManager(project).getCachedValue(project, specialFilesCacheProvider)

        // In Upsource, we create multiple instances of KotlinCacheService, which all access the same CachedValue instance (UP-8046)
        // This is so because class name of provider is used as a key when fetching cached value, see CachedValueManager.getKeyForClass.
        // To avoid race conditions, we can't use any local lock to access the cached value contents.
        return synchronized(cachedValue) {
            cachedValue.get(files)
        }
    }

    private val scriptsCacheProvider = CachedValueProvider {
        CachedValueProvider.Result(
            object : SLRUCache<Set<KtFile>, ProjectResolutionFacade>(2, 3) {
                override fun createValue(files: Set<KtFile>) = createFacadeForFilesWithSpecialModuleInfo(files)
            },
            LibraryModificationTracker.getInstance(project),
            ProjectRootModificationTracker.getInstance(project),
            ScriptDependenciesModificationTracker.getInstance(project)
        )
    }

    private fun getFacadeForScripts(files: Set<KtFile>): ProjectResolutionFacade {
        val cachedValue: SLRUCache<Set<KtFile>, ProjectResolutionFacade> =
            CachedValuesManager.getManager(project).getCachedValue(project, scriptsCacheProvider)

        return synchronized(cachedValue) {
            cachedValue.get(files)
        }
    }

    private fun getFacadeToAnalyzeFiles(files: Collection<KtFile>): ResolutionFacade {
        val file = files.first()
        val moduleInfo = file.getModuleInfo()
        val specialFiles = files.filterNotInProjectSource(moduleInfo)
        val scripts = specialFiles.filterScripts()
        if (scripts.isNotEmpty()) {
            val projectFacade = getFacadeForScripts(scripts)
            return ModuleResolutionFacadeImpl(projectFacade, moduleInfo).createdFor(scripts, moduleInfo)
        }

        if (specialFiles.isNotEmpty()) {
            val projectFacade = getFacadeForSpecialFiles(specialFiles)
            return ModuleResolutionFacadeImpl(projectFacade, moduleInfo).createdFor(specialFiles, moduleInfo)
        }

        val platform = TargetPlatformDetector.getPlatform(file)
        return getResolutionFacadeByModuleInfo(moduleInfo, platform).createdFor(emptyList(), moduleInfo, platform)
    }

    override fun getResolutionFacadeByFile(file: PsiFile, platform: TargetPlatform): ResolutionFacade? {
        if (!ProjectRootsUtil.isInProjectOrLibraryContent(file)) {
            return null
        }

        assert(file !is PsiCodeFragment)

        val moduleInfo = file.getModuleInfo()
        return getResolutionFacadeByModuleInfo(moduleInfo, platform)
    }

    private fun getResolutionFacadeByModuleInfo(moduleInfo: IdeaModuleInfo, platform: TargetPlatform): ResolutionFacade {
        val settings = moduleInfo.platformSettings(platform)
        val projectFacade = when (moduleInfo) {
            is ScriptDependenciesInfo.ForProject,
            is ScriptDependenciesSourceInfo.ForProject -> facadeForScriptDependenciesForProject
            is ScriptDependenciesInfo.ForFile -> createFacadeForScriptDependencies(moduleInfo)
            else -> globalFacade(settings)
        }
        return ModuleResolutionFacadeImpl(projectFacade, moduleInfo)
    }

    override fun getResolutionFacadeByModuleInfo(moduleInfo: ModuleInfo, platform: TargetPlatform): ResolutionFacade? =
        (moduleInfo as? IdeaModuleInfo)?.let { getResolutionFacadeByModuleInfo(it, platform) }

    private fun Collection<KtFile>.filterNotInProjectSource(moduleInfo: IdeaModuleInfo): Set<KtFile> {
        return mapNotNull {
            if (it is KtCodeFragment) it.getContextFile() else it
        }.filter {
            !ProjectRootsUtil.isInProjectSource(it) || !moduleInfo.contentScope().contains(it)
        }.toSet()
    }

    private fun Collection<KtFile>.filterScripts(): Set<KtFile> {
        return mapNotNull {
            if (it is KtCodeFragment) it.getContextFile() else it
        }.filter { it.isScript() }.toSet()
    }

    private fun KtCodeFragment.getContextFile(): KtFile? {
        val contextElement = context ?: return null
        val contextFile = (contextElement as? KtElement)?.containingKtFile
                ?: throw AssertionError("Analyzing kotlin code fragment of type ${this::class.java} with java context of type ${contextElement::class.java}")
        return if (contextFile is KtCodeFragment) contextFile.getContextFile() else contextFile
    }

    private companion object {
        private val SUPPRESS_ANNOTATION_SHORT_NAME = KotlinBuiltIns.FQ_NAMES.suppress.shortName().identifier
    }
}

