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

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.di.configureStandardResolveComponents
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.AbsentDescriptorHandler
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.resolve.scopes.optimization.OptimizingOptions
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragmentProvider
import org.jetbrains.kotlin.serialization.deserialization.MetadataPartProvider
import org.jetbrains.kotlin.storage.StorageManager

class CommonAnalysisParameters(
    val metadataPartProviderFactory: (ModuleContent<*>) -> MetadataPartProvider,
    val klibMetadataPackageFragmentProviderFactory: KlibMetadataPackageFragmentProviderFactory? = null,
) : PlatformAnalysisParameters

/**
 * A facade that is used to analyze common (platform-independent) modules in multi-platform projects.
 */
class CommonResolverForModuleFactory(
    private val platformParameters: CommonAnalysisParameters,
    private val targetEnvironment: TargetEnvironment,
    private val targetPlatform: TargetPlatform,
    private val shouldCheckExpectActual: Boolean,
    private val commonDependenciesContainer: CommonDependenciesContainer? = null
) : ResolverForModuleFactory() {
    private class SourceModuleInfo(
        override val name: Name,
        override val capabilities: Map<ModuleCapability<*>, Any?>,
        private val dependencies: Iterable<ModuleInfo>,
        override val expectedBy: List<ModuleInfo>,
        override val platform: TargetPlatform,
        private val modulesWhoseInternalsAreVisible: Collection<ModuleInfo>,
        private val dependOnOldBuiltIns: Boolean
    ) : ModuleInfo {
        override fun dependencies() = listOf(this, *dependencies.toList().toTypedArray())

        override fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> = modulesWhoseInternalsAreVisible

        override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns =
            if (dependOnOldBuiltIns) ModuleInfo.DependencyOnBuiltIns.LAST else ModuleInfo.DependencyOnBuiltIns.NONE

        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = CommonPlatformAnalyzerServices
    }

    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider,
        resolveOptimizingOptions: OptimizingOptions?,
        absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
    ): ResolverForModule {
        val (moduleInfo, syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            project, moduleContext.storageManager, syntheticFiles,
            moduleContentScope,
            moduleInfo
        )

        val metadataPartProvider = platformParameters.metadataPartProviderFactory(moduleContent)
        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()
        val container = createContainerToResolveCommonCode(
            moduleContext,
            trace,
            declarationProviderFactory,
            moduleContentScope,
            targetEnvironment,
            metadataPartProvider,
            languageVersionSettings,
            targetPlatform,
            CommonPlatformAnalyzerServices,
            shouldCheckExpectActual,
            absentDescriptorHandlerClass
        )

        val klibMetadataPackageFragmentProvider =
            platformParameters.klibMetadataPackageFragmentProviderFactory?.createPackageFragmentProvider(
                PackageFragmentProviderCreationContext(moduleInfo, moduleContext.storageManager, languageVersionSettings, moduleDescriptor)
            )

        val packageFragmentProviders =
            /** If this is a dependency module that [commonDependenciesContainer] knows about, get the package fragments from there */
            commonDependenciesContainer?.packageFragmentProviderForModuleInfo(moduleInfo)?.let(::listOf)
                ?: listOfNotNull(
                    container.get<ResolveSession>().packageFragmentProvider,
                    container.get<MetadataPackageFragmentProvider>(),
                    klibMetadataPackageFragmentProvider,
                )

        return ResolverForModule(
            CompositePackageFragmentProvider(packageFragmentProviders, "CompositeProvider@CommonResolver for $moduleDescriptor"),
            container
        )
    }

    companion object {
        fun analyzeFiles(
            files: Collection<KtFile>, moduleName: Name, dependOnBuiltIns: Boolean, languageVersionSettings: LanguageVersionSettings,
            targetPlatform: TargetPlatform,
            targetEnvironment: TargetEnvironment,
            capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),
            dependenciesContainer: CommonDependenciesContainer? = null,
            metadataPartProviderFactory: (ModuleContent<ModuleInfo>) -> MetadataPartProvider
        ): AnalysisResult {
            val moduleInfo = SourceModuleInfo(
                moduleName,
                capabilities,
                dependenciesContainer?.moduleInfos?.toList().orEmpty(),
                dependenciesContainer?.refinesModuleInfos.orEmpty(),
                targetPlatform,
                dependenciesContainer?.friendModuleInfos.orEmpty(),
                dependOnBuiltIns
            )
            val project = files.firstOrNull()?.project ?: throw AssertionError("No files to analyze")

            val multiplatformLanguageSettings = object : LanguageVersionSettings by languageVersionSettings {
                override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State =
                    if (feature == LanguageFeature.MultiPlatformProjects) LanguageFeature.State.ENABLED
                    else languageVersionSettings.getFeatureSupport(feature)
            }

            val resolverForModuleFactory = CommonResolverForModuleFactory(
                CommonAnalysisParameters(metadataPartProviderFactory),
                targetEnvironment,
                targetPlatform,
                shouldCheckExpectActual = false,
                dependenciesContainer
            )

            val projectContext = ProjectContext(project, "metadata serializer")

            val resolver = ResolverForSingleModuleProject<ModuleInfo>(
                "sources for metadata serializer",
                projectContext,
                moduleInfo,
                resolverForModuleFactory,
                GlobalSearchScope.allScope(project),
                languageVersionSettings = multiplatformLanguageSettings,
                syntheticFiles = files,
                knownDependencyModuleDescriptors = dependenciesContainer?.moduleInfos
                    ?.associateWith(dependenciesContainer::moduleDescriptorForModuleInfo).orEmpty()
            )

            val moduleDescriptor = resolver.descriptorForModule(moduleInfo)

            dependenciesContainer?.registerDependencyForAllModules(moduleInfo, moduleDescriptor)

            val container = resolver.resolverForModule(moduleInfo).componentProvider

            val analysisHandlerExtensions = AnalysisHandlerExtension.getInstances(project)
            val trace = container.get<BindingTrace>()

            // Mimic the behavior in the jvm frontend. The extensions have 2 chances to override the normal analysis:
            // * If any of the extensions returns a non-null result, it. Otherwise do the normal analysis.
            // * `analysisCompleted` can be used to override the result, too.
            var result = analysisHandlerExtensions.firstNotNullOfOrNull { extension ->
                extension.doAnalysis(project, moduleDescriptor, projectContext, files, trace, container)
            } ?: run {
                container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
                AnalysisResult.success(trace.bindingContext, moduleDescriptor)
            }

            result = analysisHandlerExtensions.firstNotNullOfOrNull { extension ->
                extension.analysisCompleted(project, moduleDescriptor, trace, files)
            } ?: result

            return result
        }
    }
}

interface CommonDependenciesContainer {
    val moduleInfos: List<ModuleInfo>

    fun moduleDescriptorForModuleInfo(moduleInfo: ModuleInfo): ModuleDescriptor

    fun registerDependencyForAllModules(
        moduleInfo: ModuleInfo,
        descriptorForModule: ModuleDescriptorImpl
    )

    fun packageFragmentProviderForModuleInfo(moduleInfo: ModuleInfo): PackageFragmentProvider?
    val friendModuleInfos: List<ModuleInfo>
    val refinesModuleInfos: List<ModuleInfo>
}

fun interface KlibMetadataPackageFragmentProviderFactory {
    fun createPackageFragmentProvider(
        context: PackageFragmentProviderCreationContext
    ): PackageFragmentProvider?
}

class PackageFragmentProviderCreationContext(
    val moduleInfo: ModuleInfo,
    val storageManager: StorageManager,
    val languageVersionSettings: LanguageVersionSettings,
    val moduleDescriptor: ModuleDescriptor,
)

private fun createContainerToResolveCommonCode(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
    moduleContentScope: GlobalSearchScope,
    targetEnvironment: TargetEnvironment,
    metadataPartProvider: MetadataPartProvider,
    languageVersionSettings: LanguageVersionSettings,
    platform: TargetPlatform,
    analyzerServices: PlatformDependentAnalyzerServices,
    shouldCheckExpectActual: Boolean,
    absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
): StorageComponentContainer =
    createContainer("ResolveCommonCode", analyzerServices) {
        configureModule(
            moduleContext,
            platform,
            analyzerServices,
            bindingTrace,
            languageVersionSettings,
            optimizingOptions = null,
            absentDescriptorHandlerClass = absentDescriptorHandlerClass
        )

        useInstance(moduleContentScope)
        useInstance(declarationProviderFactory)

        configureStandardResolveComponents()

        configureCommonSpecificComponents()
        useInstance(metadataPartProvider)

        val metadataFinderFactory = moduleContext.project.getService(
            MetadataFinderFactory::class.java
        )
            ?: error("No MetadataFinderFactory in project")
        useInstance(metadataFinderFactory.create(moduleContentScope))

        targetEnvironment.configure(this)

        if (shouldCheckExpectActual) {
            useImpl<ExpectedActualDeclarationChecker>()
        }
        useInstance(InlineConstTracker.DoNothing)
    }

fun StorageComponentContainer.configureCommonSpecificComponents() {
    useImpl<MetadataPackageFragmentProvider>()
}
