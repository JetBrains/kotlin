/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.analyzer.common.CommonAnalysisParameters
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.caches.resolve.CompositeAnalyzerServices
import org.jetbrains.kotlin.caches.resolve.CompositeResolverForModuleFactory
import org.jetbrains.kotlin.caches.resolve.resolution
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo
import org.jetbrains.kotlin.idea.compiler.IDELanguageSettingsProvider
import org.jetbrains.kotlin.idea.project.IdeaEnvironment
import org.jetbrains.kotlin.idea.project.findAnalyzerServices
import org.jetbrains.kotlin.idea.project.useCompositeAnalysis
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.platform.idePlatformKind
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.toTargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters

class IdeaResolverForProject(
    debugName: String,
    projectContext: ProjectContext,
    modules: Collection<IdeaModuleInfo>,
    private val syntheticFilesByModule: Map<IdeaModuleInfo, Collection<KtFile>>,
    delegateResolver: ResolverForProject<IdeaModuleInfo>,
    fallbackModificationTracker: ModificationTracker? = null,
    private val isReleaseCoroutines: Boolean? = null,
    // TODO(dsavvinov): this is needed only for non-composite analysis, extract separate resolver implementation instead
    private val constantSdkDependencyIfAny: SdkInfo? = null
) : AbstractResolverForProject<IdeaModuleInfo>(
    debugName,
    projectContext,
    modules,
    fallbackModificationTracker,
    delegateResolver,
    ServiceManager.getService(projectContext.project, IdePackageOracleFactory::class.java)
) {
    private val builtInsCache: BuiltInsCache =
        (delegateResolver as? IdeaResolverForProject)?.builtInsCache ?: BuiltInsCache(projectContext, this)

    override fun sdkDependency(module: IdeaModuleInfo): SdkInfo? {
        if (projectContext.project.useCompositeAnalysis) {
            require(constantSdkDependencyIfAny == null) { "Shouldn't pass SDK dependency manually for composite analysis mode" }
        }
        return constantSdkDependencyIfAny ?: module.findSdkAcrossDependencies()
    }

    override fun modulesContent(module: IdeaModuleInfo): ModuleContent<IdeaModuleInfo> =
        ModuleContent(module, syntheticFilesByModule[module] ?: emptyList(), module.contentScope())

    override fun builtInsForModule(module: IdeaModuleInfo): KotlinBuiltIns = builtInsCache.getOrCreateIfNeeded(module)

    override fun createResolverForModule(descriptor: ModuleDescriptor, moduleInfo: IdeaModuleInfo): ResolverForModule {
        val moduleContent = ModuleContent(moduleInfo, syntheticFilesByModule[moduleInfo] ?: listOf(), moduleInfo.contentScope())

        val languageVersionSettings =
            IDELanguageSettingsProvider.getLanguageVersionSettings(moduleInfo, projectContext.project, isReleaseCoroutines)

        val resolverForModuleFactory = getResolverForModuleFactory(moduleInfo)

        return resolverForModuleFactory.createResolverForModule(
            descriptor as ModuleDescriptorImpl,
            projectContext.withModule(descriptor),
            moduleContent,
            this,
            languageVersionSettings
        )
    }

    private fun getResolverForModuleFactory(moduleInfo: IdeaModuleInfo): ResolverForModuleFactory {
        val platform = moduleInfo.platform

        val jvmPlatformParameters = JvmPlatformParameters(
            packagePartProviderFactory = { IDEPackagePartProvider(it.moduleContentScope) },
            moduleByJavaClass = { javaClass: JavaClass ->
                val psiClass = (javaClass as JavaClassImpl).psi
                psiClass.getPlatformModuleInfo(JvmPlatforms.unspecifiedJvmPlatform)?.platformModule ?: psiClass.getNullableModuleInfo()
            }
        )

        val commonPlatformParameters = CommonAnalysisParameters(
            metadataPartProviderFactory = { IDEPackagePartProvider(it.moduleContentScope) }
        )

        return if (!projectContext.project.useCompositeAnalysis) {
            val parameters = when {
                platform.isJvm() -> jvmPlatformParameters
                platform.isCommon() -> commonPlatformParameters
                else -> PlatformAnalysisParameters.Empty
            }

            platform.idePlatformKind.resolution.createResolverForModuleFactory(parameters, IdeaEnvironment, platform)
        } else {
            CompositeResolverForModuleFactory(
                commonPlatformParameters,
                jvmPlatformParameters,
                platform,
                CompositeAnalyzerServices(platform.componentPlatforms.map { it.toTargetPlatform().findAnalyzerServices })
            )
        }
    }

    // Important: ProjectContext must be from SDK to be sure that we won't run into deadlocks
    class BuiltInsCache(private val projectContextFromSdkResolver: ProjectContext, private val resolverForSdk: IdeaResolverForProject) {
        private val cache = mutableMapOf<BuiltInsCacheKey, KotlinBuiltIns>()

        fun getOrCreateIfNeeded(module: IdeaModuleInfo): KotlinBuiltIns = projectContextFromSdkResolver.storageManager.compute {
            val sdk = resolverForSdk.sdkDependency(module)

            val key = module.platform.idePlatformKind.resolution.getKeyForBuiltIns(module, sdk)
            val cachedBuiltIns = cache[key]
            if (cachedBuiltIns != null) return@compute cachedBuiltIns

            // Note #1: we can't use .getOrPut, because we have to put builtIns into map *before* initialization
            // Note #2: it's OK to put not-initialized built-ins into public map, because access to [cache] is guarded by storageManager.lock
            val newBuiltIns = module.platform.idePlatformKind.resolution.createBuiltIns(module, projectContextFromSdkResolver, sdk)
            cache[key] = newBuiltIns

            if (newBuiltIns is JvmBuiltIns) {
                // SDK should be present, otherwise we wouldn't have created JvmBuiltIns in createBuiltIns
                val sdkDescriptor = resolverForSdk.descriptorForModule(sdk!!)

                val isAdditionalBuiltInsFeaturesSupported = module.supportsAdditionalBuiltInsMembers(projectContextFromSdkResolver.project)

                newBuiltIns.initialize(sdkDescriptor, isAdditionalBuiltInsFeaturesSupported)
            }

            return@compute newBuiltIns
        }
    }
}

interface BuiltInsCacheKey {
    object DefaultBuiltInsKey : BuiltInsCacheKey
}