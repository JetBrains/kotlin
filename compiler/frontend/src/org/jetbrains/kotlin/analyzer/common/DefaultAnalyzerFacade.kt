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

package org.jetbrains.kotlin.analyzer.common

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.LazyResolveToken
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureCommon
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.FileScopeProviderImpl
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragmentProvider

/**
 * A facade that is used to analyze platform independent modules in multi-platform projects.
 * See [TargetPlatform.Default]
 */
object DefaultAnalyzerFacade : AnalyzerFacade<PlatformAnalysisParameters>() {
    private val compilerConfiguration = CompilerConfiguration().apply {
        languageVersionSettings = LanguageVersionSettingsImpl(
                LanguageVersion.LATEST, ApiVersion.LATEST, setOf(LanguageFeature.MultiPlatformProjects)
        )
    }

    private class SourceModuleInfo(
            override val name: Name,
            override val capabilities: Map<ModuleDescriptor.Capability<*>, Any?>,
            private val dependOnOldBuiltIns: Boolean
    ) : ModuleInfo {
        override fun dependencies() = listOf(this)

        override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns =
                if (dependOnOldBuiltIns) ModuleInfo.DependenciesOnBuiltIns.LAST else ModuleInfo.DependenciesOnBuiltIns.NONE
    }

    fun analyzeFiles(
            files: Collection<KtFile>, moduleName: Name, dependOnBuiltIns: Boolean,
            capabilities: Map<ModuleDescriptor.Capability<*>, Any?> = mapOf(MultiTargetPlatform.CAPABILITY to MultiTargetPlatform.Common),
            packagePartProviderFactory: (ModuleInfo, ModuleContent) -> PackagePartProvider
    ): AnalysisResult {
        val moduleInfo = SourceModuleInfo(moduleName, capabilities, dependOnBuiltIns)
        val project = files.firstOrNull()?.project ?: throw AssertionError("No files to analyze")
        val resolver = setupResolverForProject(
                "sources for metadata serializer",
                ProjectContext(project), listOf(moduleInfo),
                { ModuleContent(files, GlobalSearchScope.allScope(project)) },
                object : PlatformAnalysisParameters {},
                packagePartProviderFactory = packagePartProviderFactory,
                modulePlatforms = { MultiTargetPlatform.Common }
        )

        val moduleDescriptor = resolver.descriptorForModule(moduleInfo)
        val container = resolver.resolverForModule(moduleInfo).componentProvider

        container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)

        return AnalysisResult.success(container.get<BindingTrace>().bindingContext, moduleDescriptor)
    }

    override fun <M : ModuleInfo> createResolverForModule(
            moduleInfo: M,
            moduleDescriptor: ModuleDescriptorImpl,
            moduleContext: ModuleContext,
            moduleContent: ModuleContent,
            platformParameters: PlatformAnalysisParameters,
            targetEnvironment: TargetEnvironment,
            resolverForProject: ResolverForProject<M>,
            packagePartProvider: PackagePartProvider
    ): ResolverForModule {
        val (syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
                project, moduleContext.storageManager, syntheticFiles,
                if (moduleInfo.isLibrary) GlobalSearchScope.EMPTY_SCOPE else moduleContentScope
        )

        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()
        val container = createContainerToResolveCommonCode(
                moduleContext, trace, declarationProviderFactory, moduleContentScope, targetEnvironment, packagePartProvider
        )

        val packageFragmentProviders = listOf(
                container.get<ResolveSession>().packageFragmentProvider,
                container.get<MetadataPackageFragmentProvider>()
        )

        return ResolverForModule(CompositePackageFragmentProvider(packageFragmentProviders), container)
    }

    private fun createContainerToResolveCommonCode(
            moduleContext: ModuleContext,
            bindingTrace: BindingTrace,
            declarationProviderFactory: DeclarationProviderFactory,
            moduleContentScope: GlobalSearchScope,
            targetEnvironment: TargetEnvironment,
            packagePartProvider: PackagePartProvider
    ): StorageComponentContainer = createContainer("ResolveCommonCode", targetPlatform) {
        configureModule(moduleContext, targetPlatform, bindingTrace)

        useInstance(moduleContentScope)
        useInstance(LookupTracker.DO_NOTHING)
        useImpl<ResolveSession>()
        useImpl<LazyTopDownAnalyzer>()
        useImpl<FileScopeProviderImpl>()
        configureCommon(compilerConfiguration)
        useImpl<CompilerDeserializationConfiguration>()
        useInstance(packagePartProvider)
        useInstance(declarationProviderFactory)
        useImpl<MetadataPackageFragmentProvider>()

        val metadataFinderFactory = ServiceManager.getService(moduleContext.project, MetadataFinderFactory::class.java)
                                    ?: error("No MetadataFinderFactory in project")
        useInstance(metadataFinderFactory.create(moduleContentScope))

        targetEnvironment.configure(this)
        useImpl<LazyResolveToken>()
    }

    override val targetPlatform: TargetPlatform
        get() = TargetPlatform.Default
}
