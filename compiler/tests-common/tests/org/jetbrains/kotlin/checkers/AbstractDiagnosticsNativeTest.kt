/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.di.configureStandardResolveComponents
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.storage.StorageManager
import java.util.*

abstract class AbstractDiagnosticsNativeTest : AbstractDiagnosticsTest() {

    override fun getEnvironmentConfigFiles(): EnvironmentConfigFiles = EnvironmentConfigFiles.NATIVE_CONFIG_FILES

    override fun analyzeModuleContents(
        moduleContext: ModuleContext,
        files: List<KtFile>,
        moduleTrace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
        separateModules: Boolean,
        jvmTarget: JvmTarget
    ): AnalysisResult = FakeTopDownAnalyzerFacadeForNative.analyzeFilesWithGivenTrace(
        files,
        moduleTrace,
        moduleContext,
        languageVersionSettings
    )

    override fun shouldSkipJvmSignatureDiagnostics(groupedByModule: Map<TestModule?, List<TestFile>>): Boolean = true

    override fun createModule(moduleName: String, storageManager: StorageManager): ModuleDescriptorImpl =
        ModuleDescriptorImpl(Name.special("<$moduleName>"), storageManager, DefaultBuiltIns())

    override fun createSealedModule(storageManager: StorageManager): ModuleDescriptorImpl {
        val module = createModule("kotlin-native-test-module", storageManager)

        val dependencies = ArrayList<ModuleDescriptorImpl>()

        dependencies.add(module)
        dependencies.addAll(getAdditionalDependencies(module))
        dependencies.add(module.builtIns.builtInsModule)

        module.setDependencies(dependencies)

        return module
    }
}

private object FakeTopDownAnalyzerFacadeForNative {

    fun analyzeFilesWithGivenTrace(
        files: Collection<KtFile>,
        trace: BindingTrace,
        moduleContext: ModuleContext,
        languageVersionSettings: LanguageVersionSettings
    ): AnalysisResult {

        val analyzerForNative = createFakeTopDownAnalyzerForNative(
            moduleContext, trace,
            FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
            languageVersionSettings
        )

        analyzerForNative.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
        return AnalysisResult.success(trace.bindingContext, moduleContext.module)
    }
}

private fun createFakeTopDownAnalyzerForNative(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
    languageVersionSettings: LanguageVersionSettings
): LazyTopDownAnalyzer = createContainer("FakeTopDownAnalyzerForNative", NativePlatformAnalyzerServices) {
    configureModule(
        moduleContext,
        NativePlatforms.unspecifiedNativePlatform,
        NativePlatformAnalyzerServices,
        bindingTrace,
        languageVersionSettings
    )

    configureStandardResolveComponents()

    useInstance(declarationProviderFactory)
    CompilerEnvironment.configure(this)
}.apply {
    val moduleDescriptor = get<ModuleDescriptorImpl>()
    moduleDescriptor.initialize(get<KotlinCodeAnalyzer>().packageFragmentProvider)
}.get<LazyTopDownAnalyzer>()
