/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.di.configureStandardResolveComponents
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

object FakeTopDownAnalyzerFacadeForNative {
    fun analyzeFilesWithGivenTrace(
        files: Collection<KtFile>,
        trace: BindingTrace,
        moduleContext: ModuleContext,
        languageVersionSettings: LanguageVersionSettings,
        compilerEnvironment: TargetEnvironment = CompilerEnvironment
    ): AnalysisResult {

        val analyzerForNative = createFakeTopDownAnalyzerForNative(
            moduleContext, trace,
            FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
            languageVersionSettings,
            compilerEnvironment
        )

        analyzerForNative.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files as Collection<com.intellij.psi.PsiElement>)
        return AnalysisResult.success(trace.bindingContext, moduleContext.module)
    }
}

private fun createFakeTopDownAnalyzerForNative(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
    languageVersionSettings: LanguageVersionSettings,
    compilerEnvironment: TargetEnvironment = CompilerEnvironment
): LazyTopDownAnalyzer = createContainer("FakeTopDownAnalyzerForNative", NativePlatformAnalyzerServices) {
    configureModule(
        moduleContext,
        NativePlatforms.unspecifiedNativePlatform,
        NativePlatformAnalyzerServices,
        bindingTrace,
        languageVersionSettings,
        optimizingOptions = null,
        absentDescriptorHandlerClass = null
    )

    configureStandardResolveComponents()

    useInstance(declarationProviderFactory)
    useInstance(InlineConstTracker.DoNothing)
    compilerEnvironment.configure(this)
}.apply {
    val moduleDescriptor = get<ModuleDescriptorImpl>()
    moduleDescriptor.initialize(get<KotlinCodeAnalyzer>().packageFragmentProvider)
}.get<LazyTopDownAnalyzer>()
