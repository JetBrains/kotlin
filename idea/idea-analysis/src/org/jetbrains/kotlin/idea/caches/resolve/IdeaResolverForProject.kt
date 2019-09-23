/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.analyzer.common.CommonAnalysisParameters
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.caches.resolve.CompositeAnalyzerServices
import org.jetbrains.kotlin.caches.resolve.CompositeResolverForModuleFactory
import org.jetbrains.kotlin.caches.resolve.resolution
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.findSdkAcrossDependencies
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getPlatformModuleInfo
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
    private val modules: Collection<IdeaModuleInfo>,
    private val syntheticFilesByModule: Map<IdeaModuleInfo, Collection<KtFile>>,
    delegateResolver: ResolverForProject<IdeaModuleInfo>,
    private val builtInsCache: BuiltInsCache,
    fallbackModificationTracker: ModificationTracker? = null,
    private val isReleaseCoroutines: Boolean? = null,
    // TODO(dsavvinov): this is needed only for non-composite analysis, extract separate resolver implementation instead
    private val constantSdkDependencyIfAny: IdeaModuleInfo? = null
) : AbstractResolverForProject<IdeaModuleInfo>(
    debugName,
    projectContext,
    modules,
    fallbackModificationTracker,
    delegateResolver,
    ServiceManager.getService(projectContext.project, IdePackageOracleFactory::class.java)
) {
    override fun sdkDependency(module: IdeaModuleInfo): IdeaModuleInfo? {
        if (projectContext.project.useCompositeAnalysis) {
            require(constantSdkDependencyIfAny == null) { "Shouldn't pass SDK dependency manually for composite analysis mode" }
        }
        return constantSdkDependencyIfAny ?: module.findSdkAcrossDependencies()
    }

    override fun modulesContent(module: IdeaModuleInfo): ModuleContent<IdeaModuleInfo> =
        ModuleContent(module, syntheticFilesByModule[module] ?: emptyList(), module.contentScope())

    override fun builtInsForModule(module: IdeaModuleInfo): KotlinBuiltIns {
        require(module in modules)
        val key = module.platform.idePlatformKind.resolution.getKeyForBuiltIns(module)
        return builtInsCache.getOrPut(key) { throw IllegalStateException("Can't find builtIns by key $key for module $module") }
    }

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
}