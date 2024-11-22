/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fileBelongsToModuleForLt
import org.jetbrains.kotlin.cli.common.isCommonSourceForLt
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.prepareJvmSessions
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirViaLightTree
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope

@RequiresOptIn(message = "In compiler:cli, please use FrontendContext extensions instead")
annotation class IncrementalCompilationApi

@IncrementalCompilationApi
fun compileModuleToAnalyzedFirViaLightTreeIncrementally(
    projectEnvironment: VfsBasedProjectEnvironment,
    messageCollector: MessageCollector,
    compilerConfiguration: CompilerConfiguration,
    input: ModuleCompilerInput,
    diagnosticsReporter: BaseDiagnosticsCollector,
    incrementalExcludesScope: AbstractProjectFileSearchScope?,
): FirResult {
    return MinimizedFrontendContext(
        projectEnvironment,
        messageCollector,
        FirExtensionRegistrar.getInstances(projectEnvironment.project),
        compilerConfiguration
    ).compileModuleToAnalyzedFirViaLightTreeIncrementally(
        input,
        diagnosticsReporter,
        previousStepsSymbolProviders = emptyList(),
        incrementalExcludesScope,
        friendPaths = emptyList()
    )
}

private fun FrontendContext.compileModuleToAnalyzedFirViaLightTreeIncrementally(
    input: ModuleCompilerInput,
    diagnosticsReporter: BaseDiagnosticsCollector,
    previousStepsSymbolProviders: List<FirSymbolProvider>,
    incrementalExcludesScope: AbstractProjectFileSearchScope?,
    friendPaths: List<String>,
): FirResult {
    val performanceManager = configuration[CLIConfigurationKeys.PERF_MANAGER]
    performanceManager?.notifyAnalysisStarted()

    var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()

    val incrementalCompilationScope = createIncrementalCompilationScope(
        configuration,
        projectEnvironment,
        incrementalExcludesScope
    )?.also { librariesScope -= it }

    val allSources = mutableListOf<KtSourceFile>().apply {
        addAll(input.groupedSources.commonSources)
        addAll(input.groupedSources.platformSources)
    }
    val sessionsWithSources = prepareJvmSessions(
        allSources,
        rootModuleNameAsString = input.targetId.name,
        friendPaths,
        librariesScope,
        isCommonSource = input.groupedSources.isCommonSourceForLt,
        isScript = { false },
        fileBelongsToModule = input.groupedSources.fileBelongsToModuleForLt,
        createProviderAndScopeForIncrementalCompilation = { files ->
            val scope = projectEnvironment.getSearchScopeBySourceFiles(files)
            createContextForIncrementalCompilation(
                configuration,
                projectEnvironment,
                scope,
                previousStepsSymbolProviders,
                incrementalCompilationScope
            )
        }
    )

    val countFilesAndLines = if (performanceManager == null) null else performanceManager::addSourcesStats

    val outputs = sessionsWithSources.map { (session, sources) ->
        buildResolveAndCheckFirViaLightTree(session, sources, diagnosticsReporter, countFilesAndLines)
    }
    outputs.runPlatformCheckers(diagnosticsReporter)

    performanceManager?.notifyAnalysisFinished()
    return FirResult(outputs)
}
