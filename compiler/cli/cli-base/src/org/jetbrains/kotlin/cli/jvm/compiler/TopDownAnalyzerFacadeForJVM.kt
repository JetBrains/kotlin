/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.cli.jvm.config.ClassicFrontendSpecificJvmConfigurationKeys
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.MutableModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.frontend.java.di.initJvmBuiltInsForTopDownAnalysis
import org.jetbrains.kotlin.frontend.java.di.initialize
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.jvm.jvmLibrariesProvidedByDefault
import org.jetbrains.kotlin.javac.components.JavacBasedClassFinder
import org.jetbrains.kotlin.javac.components.JavacBasedSourceElementFactory
import org.jetbrains.kotlin.javac.components.StubJavaResolverCache
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolver
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.multiplatform.OptionalAnnotationPackageFragmentProvider
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import kotlin.reflect.KFunction1

object TopDownAnalyzerFacadeForJVM {
    @JvmStatic
    @JvmOverloads
    fun analyzeFilesWithJavaIntegration(
        project: Project,
        files: Collection<KtFile>,
        trace: BindingTrace,
        configuration: CompilerConfiguration,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        declarationProviderFactory: (StorageManager, Collection<KtFile>) -> DeclarationProviderFactory = ::FileBasedDeclarationProviderFactory,
        sourceModuleSearchScope: GlobalSearchScope = newModuleSearchScope(project, files),
        klibList: List<KotlinLibrary> = emptyList(),
        explicitModuleDependencyList: List<ModuleDescriptorImpl> = emptyList(),
        explicitModuleFriendsList: List<ModuleDescriptorImpl> = emptyList(),
        explicitCompilerEnvironment: TargetEnvironment = CompilerEnvironment
    ): AnalysisResult {
        val container = createContainer(
            project, files, trace, configuration, packagePartProvider, declarationProviderFactory, explicitCompilerEnvironment,
            sourceModuleSearchScope, klibList, explicitModuleDependencyList = explicitModuleDependencyList,
            explicitModuleFriendsList = explicitModuleFriendsList
        )

        val module = container.get<ModuleDescriptor>()
        val moduleContext = container.get<ModuleContext>()

        val analysisHandlerExtensions = AnalysisHandlerExtension.getInstances(project)

        fun invokeExtensionsOnAnalysisComplete(): AnalysisResult? {
            container.get<JavaClassesTracker>().onCompletedAnalysis(module)
            for (extension in analysisHandlerExtensions) {
                val result = extension.analysisCompleted(project, module, trace, files)
                if (result != null) return result
            }

            return null
        }

        for (extension in analysisHandlerExtensions) {
            val result = extension.doAnalysis(project, module, moduleContext, files, trace, container)
            if (result != null) {
                invokeExtensionsOnAnalysisComplete()?.let { return it }
                return result
            }
        }

        container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)

        invokeExtensionsOnAnalysisComplete()?.let { return it }

        return AnalysisResult.success(trace.bindingContext, module)
    }

    fun createContainer(
        project: Project,
        files: Collection<KtFile>,
        trace: BindingTrace,
        configuration: CompilerConfiguration,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        declarationProviderFactory: (StorageManager, Collection<KtFile>) -> DeclarationProviderFactory,
        targetEnvironment: TargetEnvironment = CompilerEnvironment,
        sourceModuleSearchScope: GlobalSearchScope = newModuleSearchScope(project, files),
        klibList: List<KotlinLibrary> = emptyList(),
        implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter? = null,
        explicitModuleDependencyList: List<ModuleDescriptorImpl> = emptyList(),
        explicitModuleFriendsList: List<ModuleDescriptorImpl> = emptyList(),
        moduleCapabilities: Map<ModuleCapability<*>, Any?> = emptyMap()
    ): ComponentProvider {
        val jvmTarget = configuration.get(JVMConfigurationKeys.JVM_TARGET, JvmTarget.DEFAULT)
        val languageVersionSettings = configuration.languageVersionSettings
        val jvmPlatform = JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget)

        val moduleContext = createModuleContext(project, configuration, jvmPlatform, moduleCapabilities)

        val storageManager = moduleContext.storageManager
        val module = moduleContext.module

        val incrementalComponents = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
        val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: LookupTracker.DO_NOTHING
        val expectActualTracker = configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER) ?: ExpectActualTracker.DoNothing
        val inlineConstTracker = configuration.get(CommonConfigurationKeys.INLINE_CONST_TRACKER) ?: InlineConstTracker.DoNothing
        val enumWhenTracker = configuration.get(CommonConfigurationKeys.ENUM_WHEN_TRACKER) ?: EnumWhenTracker.DoNothing
        val targetIds = configuration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)

        val moduleClassResolver = SourceOrBinaryModuleClassResolver(sourceModuleSearchScope)

        val fallbackBuiltIns = JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FALLBACK).apply {
            initialize(builtInsModule, languageVersionSettings)
        }.builtInsModule

        fun StorageComponentContainer.useJavac() {
            useImpl<JavacBasedClassFinder>()
            useImpl<StubJavaResolverCache>()
            useImpl<JavacBasedSourceElementFactory>()
        }

        @Suppress("USELESS_CAST")
        val configureJavaClassFinder =
            if (configuration.getBoolean(JVMConfigurationKeys.USE_JAVAC)) StorageComponentContainer::useJavac
            else null as KFunction1<StorageComponentContainer, Unit>?

        val dependencyModule = run {
            val dependenciesContext = ContextForNewModule(
                moduleContext, Name.special("<dependencies of ${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>"),
                module.builtIns, jvmPlatform
            )

            // Scope for the dependency module contains everything except files present in the scope for the source module
            val dependencyScope = GlobalSearchScope.notScope(sourceModuleSearchScope)

            val dependenciesContainer = createContainerForLazyResolveWithJava(
                jvmPlatform,
                dependenciesContext, trace, DeclarationProviderFactory.EMPTY, dependencyScope, moduleClassResolver,
                targetEnvironment, lookupTracker, expectActualTracker, inlineConstTracker, enumWhenTracker,
                packagePartProvider(dependencyScope), languageVersionSettings,
                useBuiltInsProvider = true,
                configureJavaClassFinder = configureJavaClassFinder,
                implicitsResolutionFilter = implicitsResolutionFilter
            )

            moduleClassResolver.compiledCodeResolver = dependenciesContainer.get()

            dependenciesContext.setDependencies(listOf(dependenciesContext.module, fallbackBuiltIns))
            dependenciesContext.initializeModuleContents(
                CompositePackageFragmentProvider(
                    listOf(
                        moduleClassResolver.compiledCodeResolver.packageFragmentProvider,
                        dependenciesContainer.get<JvmBuiltInsPackageFragmentProvider>(),
                        dependenciesContainer.get<OptionalAnnotationPackageFragmentProvider>()
                    ),
                    "CompositeProvider@TopDownAnalyzerForJvm for dependencies ${dependenciesContext.module}"
                )
            )
            dependenciesContext.module
        }

        val partProvider = packagePartProvider(sourceModuleSearchScope).let { fragment ->
            if (targetIds == null || incrementalComponents == null) fragment
            else IncrementalPackagePartProvider(fragment, targetIds.map(incrementalComponents::getIncrementalCache))
        }

        // Note that it's necessary to create container for sources _after_ creation of container for dependencies because
        // CliLightClassGenerationSupport#initialize is invoked when container is created, so only the last module descriptor is going
        // to be stored in CliLightClassGenerationSupport, and it better be the source one (otherwise light classes would not be found)
        // TODO: get rid of duplicate invocation of CodeAnalyzerInitializer#initialize, or refactor CliLightClassGenerationSupport
        val container = createContainerForLazyResolveWithJava(
            jvmPlatform,
            moduleContext, trace, declarationProviderFactory(storageManager, files), sourceModuleSearchScope, moduleClassResolver,
            targetEnvironment, lookupTracker, expectActualTracker, inlineConstTracker, enumWhenTracker,
            partProvider, languageVersionSettings,
            useBuiltInsProvider = true,
            configureJavaClassFinder = configureJavaClassFinder,
            javaClassTracker = configuration[ClassicFrontendSpecificJvmConfigurationKeys.JAVA_CLASSES_TRACKER],
            implicitsResolutionFilter = implicitsResolutionFilter
        ).apply {
            initJvmBuiltInsForTopDownAnalysis()
            (partProvider as? IncrementalPackagePartProvider)?.deserializationConfiguration = get()
        }

        moduleClassResolver.sourceCodeResolver = container.get()
        val additionalProviders = ArrayList<PackageFragmentProvider>()

        if (incrementalComponents != null) {
            targetIds?.mapTo(additionalProviders) { targetId ->
                IncrementalPackageFragmentProvider(
                    files, module, storageManager, container.get<DeserializationComponentsForJava>().components,
                    incrementalComponents.getIncrementalCache(targetId), targetId, container.get()
                )
            }
        }

        additionalProviders.add(container.get<JavaDescriptorResolver>().packageFragmentProvider)

        // TODO: consider putting extension package fragment providers into the dependency module
        PackageFragmentProviderExtension.getInstances(project).mapNotNullTo(additionalProviders) { extension ->
            extension.getPackageFragmentProvider(project, module, storageManager, trace, null, lookupTracker)
        }

        val klibModules = getKlibModules(klibList, dependencyModule)

        // TODO: remove dependencyModule from friends
        module.setDependencies(
            listOf(module, dependencyModule, fallbackBuiltIns) + klibModules + explicitModuleDependencyList,
            setOf(dependencyModule) + explicitModuleFriendsList,
        )
        module.initialize(
            CompositePackageFragmentProvider(
                listOf(
                    container.get<KotlinCodeAnalyzer>().packageFragmentProvider,
                    container.get<OptionalAnnotationPackageFragmentProvider>()
                ) + additionalProviders,
                "CompositeProvider@TopDownAnalzyerForJvm for $module"
            )
        )

        return container
    }

    fun newModuleSearchScope(project: Project, files: Collection<KtFile>): GlobalSearchScope {
        // In case of separate modules, the source module scope generally consists of the following scopes:
        // 1) scope which only contains passed Kotlin source files (.kt and .kts)
        // 2) scope which contains all Java source files (.java) in the project
        return GlobalSearchScope.filesScope(project, files.map { it.virtualFile }.toSet()).uniteWith(AllJavaSourcesInProjectScope(project))
    }

    // TODO: limit this scope to the Java source roots, which the module has in its CONTENT_ROOTS
    class AllJavaSourcesInProjectScope(project: Project) : DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
        // 'isDirectory' check is needed because otherwise directories such as 'frontend.java' would be recognized
        // as Java source files, which makes no sense
        override fun contains(file: VirtualFile) =
            (file.extension == JavaFileType.DEFAULT_EXTENSION || file.fileType === JavaFileType.INSTANCE) && !file.isDirectory

        override fun toString() = "All Java sources in the project"
    }

    class SourceOrBinaryModuleClassResolver(private val sourceScope: GlobalSearchScope) : ModuleClassResolver {
        lateinit var compiledCodeResolver: JavaDescriptorResolver
        lateinit var sourceCodeResolver: JavaDescriptorResolver

        override fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
            val resolver = if (javaClass is VirtualFileBoundJavaClass && javaClass.isFromSourceCodeInScope(sourceScope))
                sourceCodeResolver
            else
                compiledCodeResolver
            return resolver.resolveClass(javaClass)
        }
    }

    private fun createModuleContext(
        project: Project,
        configuration: CompilerConfiguration,
        platform: TargetPlatform?,
        capabilities: Map<ModuleCapability<*>, Any?> = emptyMap()
    ): MutableModuleContext {
        val projectContext = ProjectContext(project, "TopDownAnalyzer for JVM")
        val builtIns = JvmBuiltIns(projectContext.storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES)
        return ContextForNewModule(
            projectContext, Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>"),
            builtIns, platform, capabilities
        ).apply {
            builtIns.builtInsModule = module
        }
    }
}

// From serialization.js....klib.kt

val jvmFactories = KlibMetadataFactories(
    { storageManager -> JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FROM_DEPENDENCIES) },
    NullFlexibleTypeDeserializer
)

private fun getKlibModules(klibList: List<KotlinLibrary>, dependencyModule: ModuleDescriptorImpl?): List<ModuleDescriptorImpl> {
    val descriptorMap = mutableMapOf<String, ModuleDescriptorImpl>()
    return klibList.map { library ->
        descriptorMap.getOrPut(library.libraryName) { getModuleDescriptorByLibrary(library, descriptorMap, dependencyModule) }
    }
}

private fun getModuleDescriptorByLibrary(
    current: KotlinLibrary, mapping:
    Map<String, ModuleDescriptorImpl>,
    dependencyModule: ModuleDescriptorImpl?
): ModuleDescriptorImpl {
    val module = jvmFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
        current,
        LanguageVersionSettingsImpl.DEFAULT,
        LockBasedStorageManager.NO_LOCKS,
        null,
        packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
        lookupTracker = LookupTracker.DO_NOTHING
    )

    val dependencies = current.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).mapNotNull {
        mapping[it] ?: run {
            assert(it in jvmLibrariesProvidedByDefault) { "Unknown library $it" }
            null
        }
    }

    module.setDependencies(listOf(module) + dependencies + listOfNotNull(dependencyModule))
    return module
}
