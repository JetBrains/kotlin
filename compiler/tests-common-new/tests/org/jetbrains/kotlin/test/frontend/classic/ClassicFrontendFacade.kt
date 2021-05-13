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
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JVMConfigurationKeys.JVM_TARGET
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.frontend.java.di.initJvmBuiltInsForTopDownAnalysis
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.native.FakeTopDownAnalyzerFacadeForNative
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.serialization.deserialization.MetadataPartProvider
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

class ClassicFrontendFacade(
    testServices: TestServices
) : FrontendFacade<ClassicFrontendOutputArtifact>(testServices, FrontendKinds.ClassicFrontend) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::ModuleDescriptorProvider))

    override fun analyze(module: TestModule): ClassicFrontendOutputArtifact {
        val dependencyProvider = testServices.dependencyProvider
        val moduleDescriptorProvider = testServices.moduleDescriptorProvider
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val packagePartProviderFactory = compilerConfigurationProvider.getPackagePartProviderFactory(module)
        val project = compilerConfigurationProvider.getProject(module)
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)

        val ktFilesMap = testServices.sourceFileProvider.getKtFilesForSourceFiles(module.files, project).toMutableMap()
        val languageVersionSettings = module.languageVersionSettings

        val sourceDependencies = module.allDependencies.filter { it.kind == DependencyKind.Source }
        val dependentDescriptors = sourceDependencies.mapNotNull {
            val testModule = dependencyProvider.getTestModule(it.moduleName)
            if (testModule.targetPlatform.isCommon()) return@mapNotNull null
            moduleDescriptorProvider.getModuleDescriptor(testModule)
        }

        val friendDescriptors = module.friendDependencies.filter { it.kind == DependencyKind.Source }.map {
            moduleDescriptorProvider.getModuleDescriptor(dependencyProvider.getTestModule(it.moduleName))
        }

        var hasCommonModules = false

        fun addDependsOnSources(dependencies: List<DependencyDescription>) {
            if (dependencies.isEmpty()) return
            hasCommonModules = true
            for (dependency in dependencies) {
                val dependencyModule = dependencyProvider.getTestModule(dependency.moduleName)
                val artifact = dependencyProvider.getArtifact(dependencyModule, FrontendKinds.ClassicFrontend)
                /*
                 * We need create KtFiles again with new project because otherwise we can access to some caches using
                 *   old project as key which may leads to missing services in core environment
                 */
                val ktFiles = testServices.sourceFileProvider.getKtFilesForSourceFiles(artifact.allKtFiles.keys, project)
                ktFiles.values.forEach { ktFile -> ktFile.isCommonSource = true }
                ktFilesMap.putAll(ktFiles)
                addDependsOnSources(dependencyModule.dependsOnDependencies)
            }
        }

        addDependsOnSources(module.dependsOnDependencies)

        val ktFiles = ktFilesMap.values.toList()
        setupJavacIfNeeded(module, ktFiles, configuration)
        val analysisResult = performResolve(
            module,
            project,
            configuration,
            packagePartProviderFactory,
            ktFiles,
            dependentDescriptors,
            friendDescriptors,
            hasCommonModules
        )
        moduleDescriptorProvider.replaceModuleDescriptorForModule(module, analysisResult.moduleDescriptor)
        return ClassicFrontendOutputArtifact(
            ktFilesMap,
            analysisResult,
            project,
            languageVersionSettings
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
        dependentDescriptors: List<ModuleDescriptorImpl>,
        friendsDescriptors: List<ModuleDescriptorImpl>,
        hasCommonModules: Boolean
    ): AnalysisResult {
        val targetPlatform = module.targetPlatform
        return when {
            targetPlatform.isJvm() -> performJvmModuleResolve(
                module,
                project,
                configuration,
                packagePartProviderFactory,
                files,
                dependentDescriptors,
                friendsDescriptors,
                hasCommonModules
            )
            targetPlatform.isJs() -> performJsModuleResolve(project, configuration, files, dependentDescriptors)
            targetPlatform.isNative() -> performNativeModuleResolve(module, project, files)
            targetPlatform.isCommon() -> performCommonModuleResolve(module, files)
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
        dependentDescriptors: List<ModuleDescriptorImpl>,
        friendsDescriptors: List<ModuleDescriptorImpl>,
        hasCommonModules: Boolean
    ): AnalysisResult {
        val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(project)
        configuration.getList(JVMConfigurationKeys.FRIEND_PATHS).forEach { moduleVisibilityManager.addFriendPath(it) }

        val moduleTrace = NoScopeRecordCliBindingTrace()
        if (!hasCommonModules) {
            return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project,
                files,
                moduleTrace,
                configuration.copy(),
                packagePartProviderFactory,
                explicitModuleDependencyList = dependentDescriptors,
                explicitModuleFriendsList = friendsDescriptors
            )
        }

        val moduleContentScope = GlobalSearchScope.allScope(project)
        val moduleClassResolver = SingleModuleClassResolver()
        val moduleContext = createModuleContext(module, project, dependentDescriptors, friendsDescriptors) {
            JvmBuiltIns(it, JvmBuiltIns.Kind.FROM_CLASS_LOADER)
        }
        val moduleDescriptor = moduleContext.module as ModuleDescriptorImpl
        val jvmTarget = configuration[JVM_TARGET] ?: JvmTarget.DEFAULT
        val container = createContainerForLazyResolveWithJava(
            JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget), // TODO(dsavvinov): do not pass JvmTarget around
            moduleContext,
            moduleTrace,
            FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
            moduleContentScope,
            moduleClassResolver,
            CompilerEnvironment, LookupTracker.DO_NOTHING,
            ExpectActualTracker.DoNothing,
            packagePartProviderFactory(moduleContentScope),
            module.languageVersionSettings,
            useBuiltInsProvider = true
        )

        container.initJvmBuiltInsForTopDownAnalysis()
        moduleClassResolver.resolver = container.get()

        moduleDescriptor.initialize(
            CompositePackageFragmentProvider(
                listOf(
                    container.get<KotlinCodeAnalyzer>().packageFragmentProvider,
                    container.get<JavaDescriptorResolver>().packageFragmentProvider
                )
            )
        )

        container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)

        return AnalysisResult.success(moduleTrace.bindingContext, moduleDescriptor)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun performJsModuleResolve(
        project: Project,
        configuration: CompilerConfiguration,
        files: List<KtFile>,
        dependentDescriptors: List<ModuleDescriptorImpl>
    ): AnalysisResult {
        val jsConfig = JsConfig(project, configuration, CompilerEnvironment)
        val dependentDescriptorsIncludingLibraries = buildList {
            addAll(dependentDescriptors)
            addAll(jsConfig.moduleDescriptors)
        }
        return TopDownAnalyzerFacadeForJS.analyzeFiles(
            files,
            project,
            configuration,
            moduleDescriptors = dependentDescriptorsIncludingLibraries,
            friendModuleDescriptors = emptyList(),
            CompilerEnvironment,
        )
    }

    private fun performNativeModuleResolve(
        module: TestModule,
        project: Project,
        files: List<KtFile>,
    ): AnalysisResult {
        val moduleTrace = NoScopeRecordCliBindingTrace()
        val moduleContext = createModuleContext(module, project, dependentDescriptors = emptyList(), friendsDescriptors = emptyList()) {
            DefaultBuiltIns()
        }
        return FakeTopDownAnalyzerFacadeForNative.analyzeFilesWithGivenTrace(
            files,
            moduleTrace,
            moduleContext,
            module.languageVersionSettings
        )
    }

    private fun performCommonModuleResolve(
        module: TestModule,
        files: List<KtFile>,
    ): AnalysisResult {
        return CommonResolverForModuleFactory.analyzeFiles(
            files,
            Name.special("<${module.name}>"),
            dependOnBuiltIns = true,
            module.languageVersionSettings,
            module.targetPlatform,
            CompilerEnvironment,
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
        dependentDescriptors: List<ModuleDescriptorImpl>,
        friendsDescriptors: List<ModuleDescriptorImpl>,
        builtInsFactory: (StorageManager) -> KotlinBuiltIns,
    ): ModuleContext {
        val projectContext = ProjectContext(project, "test project context")
        val storageManager = projectContext.storageManager

        val builtIns = builtInsFactory(storageManager)
        val moduleDescriptor = ModuleDescriptorImpl(Name.special("<${module.name}>"), storageManager, builtIns, module.targetPlatform)
        val dependencies = buildList {
            add(moduleDescriptor)
            add(moduleDescriptor.builtIns.builtInsModule)
            addAll(dependentDescriptors)
        }
        moduleDescriptor.setDependencies(dependencies, friendsDescriptors.toSet())

        return projectContext.withModule(moduleDescriptor)
    }
}
