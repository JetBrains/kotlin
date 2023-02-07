/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForJSIR
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JVMConfigurationKeys.JVM_TARGET
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDependenciesImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.frontend.java.di.initJvmBuiltInsForTopDownAnalysis
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.web.*
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.native.FakeTopDownAnalyzerFacadeForNative
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.serialization.deserialization.MetadataPartProvider
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.MultiplatformDiagnosticsDirectives.ENABLE_MULTIPLATFORM_COMPOSITE_ANALYSIS_MODE
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.types.typeUtil.closure
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

class ClassicFrontendFacade(
    testServices: TestServices
) : FrontendFacade<ClassicFrontendOutputArtifact>(testServices, FrontendKinds.ClassicFrontend) {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::ModuleDescriptorProvider))

    private val multiplatformAnalysisConfiguration by lazy {
        MultiplatformAnalysisConfiguration(testServices)
    }

    override fun analyze(module: TestModule): ClassicFrontendOutputArtifact {
        val moduleDescriptorProvider = testServices.moduleDescriptorProvider
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)
        val project = compilerConfigurationProvider.getProject(module)
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)

        val ktFilesMap = multiplatformAnalysisConfiguration.getKtFilesForForSourceFiles(project, module)
        val ktFiles = ktFilesMap.values.toList()

        setupJavacIfNeeded(module, ktFiles, configuration)
        @Suppress("UNCHECKED_CAST")
        val analysisResult = performResolve(
            module,
            project,
            configuration,
            packagePartProviderFactory,
            ktFiles,
            compilerEnvironment = multiplatformAnalysisConfiguration.getCompilerEnvironment(module),
            dependencyDescriptors = multiplatformAnalysisConfiguration.getDependencyDescriptors(module) as List<ModuleDescriptorImpl>,
            friendsDescriptors = multiplatformAnalysisConfiguration.getFriendDescriptors(module) as List<ModuleDescriptorImpl>,
            dependsOnDescriptors = multiplatformAnalysisConfiguration.getDependsOnDescriptors(module) as List<ModuleDescriptorImpl>
        )
        moduleDescriptorProvider.replaceModuleDescriptorForModule(module, analysisResult.moduleDescriptor)
        return ClassicFrontendOutputArtifact(
            ktFilesMap,
            analysisResult,
            project,
            module.languageVersionSettings
        )
    }

    private fun setupJavacIfNeeded(
        module: TestModule,
        ktFiles: List<KtFile>,
        configuration: CompilerConfiguration
    ) {
        if (JvmEnvironmentConfigurationDirectives.USE_JAVAC !in module.directives) return
        val mockJdk = runIf(JvmEnvironmentConfigurationDirectives.FULL_JDK !in module.directives) {
            File(KtTestUtil.getHomeDirectory(), "compiler/testData/mockJDK/jre/lib/rt.jar")
        }
        testServices.compilerConfigurationProvider.registerJavacForModule(module, ktFiles, mockJdk)
        configuration.put(JVMConfigurationKeys.USE_JAVAC, true)
    }

    private fun performResolve(
        module: TestModule,
        project: Project,
        configuration: CompilerConfiguration,
        packagePartProviderFactory: (GlobalSearchScope) -> JvmPackagePartProvider,
        files: List<KtFile>,
        compilerEnvironment: TargetEnvironment,
        dependencyDescriptors: List<ModuleDescriptorImpl>,
        friendsDescriptors: List<ModuleDescriptorImpl>,
        dependsOnDescriptors: List<ModuleDescriptorImpl>
    ): AnalysisResult {
        val targetPlatform = module.targetPlatform
        return when {
            targetPlatform.isJvm() -> performJvmModuleResolve(
                module, project, configuration, packagePartProviderFactory,
                files, compilerEnvironment, dependencyDescriptors, friendsDescriptors, dependencyDescriptors
            )
            targetPlatform.isJs() -> when {
                module.targetBackend?.isIR != true -> performJsModuleResolve(
                    project, configuration, compilerEnvironment, files, dependencyDescriptors
                )
                else -> performJsIrModuleResolve(
                    module, project, configuration, compilerEnvironment, files, dependencyDescriptors, friendsDescriptors
                )
            }
            targetPlatform.isNative() -> performNativeModuleResolve(
                module, project, compilerEnvironment, files, dependencyDescriptors, friendsDescriptors, dependsOnDescriptors
            )
            targetPlatform.isCommon() -> performCommonModuleResolve(module, compilerEnvironment, files)
            else -> error("Should not be here")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun performJvmModuleResolve(
        module: TestModule,
        project: Project,
        configuration: CompilerConfiguration,
        packagePartProviderFactory: (GlobalSearchScope) -> JvmPackagePartProvider,
        files: List<KtFile>,
        compilerEnviornment: TargetEnvironment,
        dependencyDescriptors: List<ModuleDescriptorImpl>,
        friendsDescriptors: List<ModuleDescriptorImpl>,
        dependsOnDescriptors: List<ModuleDescriptorImpl>
    ): AnalysisResult {
        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(project)
        configuration.getList(JVMConfigurationKeys.FRIEND_PATHS).forEach { moduleVisibilityManager.addFriendPath(it) }

        val moduleTrace = NoScopeRecordCliBindingTrace()
        if (module.dependsOnDependencies.isEmpty()) {
            return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project,
                files,
                moduleTrace,
                configuration.copy(),
                packagePartProviderFactory,
                explicitModuleDependencyList = dependencyDescriptors,
                explicitModuleFriendsList = friendsDescriptors,
                explicitCompilerEnvironment = compilerEnviornment
            )
        }

        val moduleContentScope = GlobalSearchScope.allScope(project)
        val moduleClassResolver = SingleModuleClassResolver()
        val moduleContext = createModuleContext(module, project, dependencyDescriptors, friendsDescriptors, dependsOnDescriptors) {
            JvmBuiltIns(it, JvmBuiltIns.Kind.FROM_CLASS_LOADER)
        }
        val moduleDescriptor = moduleContext.module as ModuleDescriptorImpl
        val jvmTarget = configuration[JVM_TARGET] ?: JvmTarget.DEFAULT
        val container = createContainerForLazyResolveWithJava(
            jvmPlatform = JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget), // TODO(dsavvinov): do not pass JvmTarget around
            moduleContext = moduleContext,
            bindingTrace = moduleTrace,
            declarationProviderFactory = FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
            moduleContentScope = moduleContentScope,
            moduleClassResolver = moduleClassResolver,
            targetEnvironment = compilerEnviornment,
            lookupTracker = LookupTracker.DO_NOTHING,
            expectActualTracker = ExpectActualTracker.DoNothing,
            inlineConstTracker = InlineConstTracker.DoNothing,
            enumWhenTracker = EnumWhenTracker.DoNothing,
            packagePartProvider = packagePartProviderFactory(moduleContentScope),
            languageVersionSettings = module.languageVersionSettings,
            useBuiltInsProvider = true
        )

        container.initJvmBuiltInsForTopDownAnalysis()
        moduleClassResolver.resolver = container.get()

        moduleDescriptor.initialize(
            CompositePackageFragmentProvider(
                listOf(
                    container.get<KotlinCodeAnalyzer>().packageFragmentProvider,
                    container.get<JavaDescriptorResolver>().packageFragmentProvider
                ),
                "CompositeProvider@ClassicFrontendFacade for $moduleDescriptor"
            )
        )

        container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)

        return AnalysisResult.success(moduleTrace.bindingContext, moduleDescriptor)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun performJsModuleResolve(
        project: Project,
        configuration: CompilerConfiguration,
        compilerEnvironment: TargetEnvironment,
        files: List<KtFile>,
        dependencyDescriptors: List<ModuleDescriptorImpl>
    ): AnalysisResult {
        // `dependencyDescriptors` - modules with source dependency kind
        // 'jsConfig.moduleDescriptors' - modules with binary dependency kind
        val jsConfig = JsEnvironmentConfigurator.createJsConfig(project, configuration, compilerEnvironment)
        return TopDownAnalyzerFacadeForJS.analyzeFiles(
            files,
            project = jsConfig.project,
            configuration = jsConfig.configuration,
            moduleDescriptors = dependencyDescriptors + jsConfig.moduleDescriptors,
            friendModuleDescriptors = jsConfig.friendModuleDescriptors,
            targetEnvironment = jsConfig.targetEnvironment,
        )
    }

    private fun loadKlib(names: List<String>, configuration: CompilerConfiguration): List<ModuleDescriptor> {
        val resolvedLibraries = jsResolveLibraries(
            names,
            configuration.resolverLogger
        ).getFullResolvedList()

        var builtInsModule: KotlinBuiltIns? = null
        val dependencies = mutableListOf<ModuleDescriptorImpl>()

        return resolvedLibraries.map { resolvedLibrary ->
            testServices.jsLibraryProvider.getOrCreateStdlibByPath(resolvedLibrary.library.libraryName) {
                val storageManager = LockBasedStorageManager("ModulesStructure")
                val isBuiltIns = resolvedLibrary.library.unresolvedDependencies.isEmpty()

                val moduleDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                    resolvedLibrary.library,
                    configuration.languageVersionSettings,
                    storageManager,
                    builtInsModule,
                    packageAccessHandler = null,
                    lookupTracker = LookupTracker.DO_NOTHING
                )
                if (isBuiltIns) builtInsModule = moduleDescriptor.builtIns
                dependencies += moduleDescriptor
                moduleDescriptor.setDependencies(ArrayList(dependencies))

                Pair(moduleDescriptor, resolvedLibrary.library)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun performJsIrModuleResolve(
        module: TestModule,
        project: Project,
        configuration: CompilerConfiguration,
        compilerEnvironment: TargetEnvironment,
        files: List<KtFile>,
        dependencyDescriptors: List<ModuleDescriptor>,
        friendsDescriptors: List<ModuleDescriptor>,
    ): AnalysisResult {
        val runtimeKlibsNames = JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices)
        val runtimeKlibs = loadKlib(runtimeKlibsNames, configuration)
        val transitiveLibraries = JsEnvironmentConfigurator.getDependencies(module, testServices, DependencyRelation.RegularDependency)
        val friendLibraries = JsEnvironmentConfigurator.getDependencies(module, testServices, DependencyRelation.FriendDependency)
        val allDependencies = runtimeKlibs + dependencyDescriptors + friendLibraries + friendsDescriptors + transitiveLibraries

        val analyzer = AnalyzerWithCompilerReport(configuration)
        val builtInModuleDescriptor = allDependencies.firstNotNullOfOrNull { it.builtIns }?.builtInsModule
        analyzer.analyzeAndReport(files) {
            TopDownAnalyzerFacadeForJSIR.analyzeFiles(
                files,
                project,
                configuration,
                allDependencies,
                friendsDescriptors + friendLibraries,
                compilerEnvironment,
                thisIsBuiltInsModule = builtInModuleDescriptor == null,
                customBuiltInsModule = builtInModuleDescriptor
            )
        }

        return analyzer.analysisResult
    }

    private fun performNativeModuleResolve(
        module: TestModule,
        project: Project,
        compilerEnvironment: TargetEnvironment,
        files: List<KtFile>,
        dependencyDescriptors: List<ModuleDescriptorImpl>,
        friendsDescriptors: List<ModuleDescriptorImpl>,
        dependsOnDescriptors: List<ModuleDescriptorImpl>,
    ): AnalysisResult {
        val moduleTrace = NoScopeRecordCliBindingTrace()
        val moduleContext = createModuleContext(
            module, project,
            dependencyDescriptors = dependencyDescriptors,
            friendsDescriptors = friendsDescriptors,
            dependsOnDescriptors = dependsOnDescriptors
        ) {
            DefaultBuiltIns()
        }
        return FakeTopDownAnalyzerFacadeForNative.analyzeFilesWithGivenTrace(
            files,
            moduleTrace,
            moduleContext,
            module.languageVersionSettings,
            compilerEnvironment
        )
    }

    private fun performCommonModuleResolve(
        module: TestModule,
        compilerEnvironment: TargetEnvironment,
        files: List<KtFile>,
    ): AnalysisResult {
        // See 'TODO' for adding dependency manager
        require(module.dependsOnDependencies.isEmpty() || ENABLE_MULTIPLATFORM_COMPOSITE_ANALYSIS_MODE !in module.directives) {
            "Analyzing common modules with 'dependsOn' edges in ${ENABLE_MULTIPLATFORM_COMPOSITE_ANALYSIS_MODE.name} is not supported yet.\n" +
                    "Module: ${module.name}\n" +
                    "dependsOn: ${module.dependsOnDependencies.map { it.moduleName }}"
        }
        return CommonResolverForModuleFactory.analyzeFiles(
            files,
            Name.special("<${module.name}>"),
            dependOnBuiltIns = true,
            module.languageVersionSettings,
            module.targetPlatform,
            compilerEnvironment,
            // TODO: add dependency manager
        ) {
            // TODO
            MetadataPartProvider.Empty
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun createModuleContext(
        module: TestModule,
        project: Project,
        dependencyDescriptors: List<ModuleDescriptorImpl>,
        friendsDescriptors: List<ModuleDescriptorImpl>,
        dependsOnDescriptors: List<ModuleDescriptorImpl>,
        builtInsFactory: (StorageManager) -> KotlinBuiltIns,
    ): ModuleContext {
        val projectContext = ProjectContext(project, "test project context")
        val storageManager = projectContext.storageManager

        val builtIns = builtInsFactory(storageManager)
        val moduleDescriptor = ModuleDescriptorImpl(Name.special("<${module.name}>"), storageManager, builtIns, module.targetPlatform)
        val dependencies = buildList {
            add(moduleDescriptor)
            add(moduleDescriptor.builtIns.builtInsModule)
            addAll(dependencyDescriptors)
        }
        moduleDescriptor.setDependencies(
            ModuleDependenciesImpl(
                allDependencies = dependencies,
                modulesWhoseInternalsAreVisible = friendsDescriptors.toSet(),
                directExpectedByDependencies = dependsOnDescriptors,
                allExpectedByDependencies = dependsOnDescriptors.closure { dependsOnModuleDescriptor ->
                    dependsOnModuleDescriptor.expectedByModules.map { it as ModuleDescriptorImpl }
                }.toSet()
            ),
        )
        return projectContext.withModule(moduleDescriptor)
    }
}
