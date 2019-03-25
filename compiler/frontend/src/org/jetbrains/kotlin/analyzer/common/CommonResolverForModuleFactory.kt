/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.analyzer.common

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.TargetPlatformVersion
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.createContainerToResolveCommonCode
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragmentProvider
import org.jetbrains.kotlin.serialization.deserialization.MetadataPartProvider

class CommonAnalysisParameters(
    val metadataPartProviderFactory: (ModuleContent<*>) -> MetadataPartProvider
) : PlatformAnalysisParameters

/**
 * A facade that is used to analyze common (platform-independent) modules in multi-platform projects.
 * See [CommonPlatform]
 */
object CommonResolverForModuleFactory : ResolverForModuleFactory() {
    private class SourceModuleInfo(
        override val name: Name,
        override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>,
        private val dependOnOldBuiltIns: Boolean
    ) : ModuleInfo {
        override fun dependencies() = listOf(this)

        override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns =
            if (dependOnOldBuiltIns) ModuleInfo.DependencyOnBuiltIns.LAST else ModuleInfo.DependencyOnBuiltIns.NONE

        override val platform: TargetPlatform
            get() = DefaultBuiltInPlatforms.commonPlatform

        override val compilerServices: PlatformDependentCompilerServices
            get() = CommonPlatformCompilerServices
    }

    fun analyzeFiles(
        files: Collection<KtFile>, moduleName: Name, dependOnBuiltIns: Boolean, languageVersionSettings: LanguageVersionSettings,
        capabilities: Map<ModuleDescriptor.Capability<*>, Any?> = emptyMap(),
        metadataPartProviderFactory: (ModuleContent<ModuleInfo>) -> MetadataPartProvider
    ): AnalysisResult {
        val moduleInfo = SourceModuleInfo(moduleName, capabilities, dependOnBuiltIns)
        val project = files.firstOrNull()?.project ?: throw AssertionError("No files to analyze")

        val multiplatformLanguageSettings = object : LanguageVersionSettings by languageVersionSettings {
            override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State =
                if (feature == LanguageFeature.MultiPlatformProjects) LanguageFeature.State.ENABLED
                else languageVersionSettings.getFeatureSupport(feature)
        }

        @Suppress("NAME_SHADOWING")
        val resolver = ResolverForProjectImpl(
            "sources for metadata serializer",
            ProjectContext(project, "metadata serializer"),
            listOf(moduleInfo),
            modulesContent = { ModuleContent(it, files, GlobalSearchScope.allScope(project)) },
            moduleLanguageSettingsProvider = object : LanguageSettingsProvider {
                override fun getLanguageVersionSettings(
                    moduleInfo: ModuleInfo,
                    project: Project,
                    isReleaseCoroutines: Boolean?
                ) = multiplatformLanguageSettings

                override fun getTargetPlatform(
                    moduleInfo: ModuleInfo,
                    project: Project
                ) = TargetPlatformVersion.NoVersion
            },
            resolverForModuleFactoryByPlatform = { CommonResolverForModuleFactory },
            platformParameters = { _ -> CommonAnalysisParameters(metadataPartProviderFactory) }
        )

        val moduleDescriptor = resolver.descriptorForModule(moduleInfo)
        val container = resolver.resolverForModule(moduleInfo).componentProvider

        container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)

        return AnalysisResult.success(container.get<BindingTrace>().bindingContext, moduleDescriptor)
    }

    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        platformParameters: PlatformAnalysisParameters,
        targetEnvironment: TargetEnvironment,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings
    ): ResolverForModule {
        val (moduleInfo, syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            project, moduleContext.storageManager, syntheticFiles,
            moduleContentScope,
            moduleInfo
        )

        val metadataPartProvider = (platformParameters as CommonAnalysisParameters).metadataPartProviderFactory(moduleContent)
        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()
        val container = createContainerToResolveCommonCode(
            moduleContext, trace, declarationProviderFactory, moduleContentScope, targetEnvironment, metadataPartProvider,
            languageVersionSettings, moduleInfo.platform!!, CommonPlatformCompilerServices
        )

        val packageFragmentProviders = listOf(
            container.get<ResolveSession>().packageFragmentProvider,
            container.get<MetadataPackageFragmentProvider>()
        )

        return ResolverForModule(CompositePackageFragmentProvider(packageFragmentProviders), container)
    }
}