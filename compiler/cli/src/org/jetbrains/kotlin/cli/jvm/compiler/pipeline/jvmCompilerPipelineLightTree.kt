/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.pipeline

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.GroupedKtSources
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForLightTree
import org.jetbrains.kotlin.cli.common.collectSources
import org.jetbrains.kotlin.cli.common.fileBelongsToModuleForLt
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.isCommonSourceForLt
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.prepareJvmSessions
import org.jetbrains.kotlin.cli.jvm.compiler.FirKotlinToJvmBytecodeCompiler.createPendingReporter
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.BackendInputForMultiModuleChunk
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.toBackendInput
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.runBackend
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.applyModuleProperties
import org.jetbrains.kotlin.cli.jvm.compiler.findMainClass
import org.jetbrains.kotlin.cli.jvm.compiler.writeOutputsIfNeeded
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirViaLightTree
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

fun compileModulesUsingFrontendIrAndLightTree(
    projectEnvironment: VfsBasedProjectEnvironment,
    compilerConfiguration: CompilerConfiguration,
    messageCollector: MessageCollector,
    buildFile: File?,
    chunk: List<Module>,
    targetDescription: String,
    checkSourceFiles: Boolean,
    isPrintingVersion: Boolean,
): Boolean {
    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    val performanceManager = compilerConfiguration[CLIConfigurationKeys.PERF_MANAGER]
    performanceManager?.notifyCompilerInitialized(0, 0, targetDescription)

    val project = projectEnvironment.project
    FirAnalysisHandlerExtension.analyze(project, compilerConfiguration)?.let { return it }

    val groupedSources = collectSources(compilerConfiguration, projectEnvironment, messageCollector)
    if (messageCollector.hasErrors()) {
        return false
    }

    if (checkSourceFiles && groupedSources.isEmpty() && buildFile == null) {
        if (isPrintingVersion) return true
        messageCollector.report(CompilerMessageSeverity.ERROR, "No source files")
        return false
    }

    return if (chunk.size == 1) {
        compileSingleModuleUsingFrontendIrAndLightTree(
            project,
            projectEnvironment,
            compilerConfiguration,
            messageCollector,
            buildFile,
            chunk.single(),
            groupedSources,
        )
    } else {
        compileMultiModuleChunkUsingFrontendIrAndLightTree(
            project,
            projectEnvironment,
            compilerConfiguration,
            messageCollector,
            buildFile,
            chunk,
            groupedSources,
            targetDescription,
        )
    }
}

private fun compileMultiModuleChunkUsingFrontendIrAndLightTree(
    project: Project,
    projectEnvironment: VfsBasedProjectEnvironment,
    compilerConfiguration: CompilerConfiguration,
    messageCollector: MessageCollector,
    buildFile: File?,
    chunk: List<Module>,
    groupedSources: GroupedKtSources,
    targetDescription: String,
): Boolean {
    val friendPaths = compilerConfiguration.getList(JVMConfigurationKeys.FRIEND_PATHS)
    val moduleVisibilityManager = ModuleVisibilityManager.SERVICE.getInstance(project)
    for (path in friendPaths) {
        moduleVisibilityManager.addFriendPath(path)
    }

    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)
    val frontendContext = createFrontendContextForMultiChunkMode(
        projectEnvironment, messageCollector, compilerConfiguration, project
    )

    val firResult = with(frontendContext) {
        // K2/LT: frontend
        val performanceManager = configuration.get<CommonCompilerPerformanceManager>(CLIConfigurationKeys.PERF_MANAGER)
        performanceManager?.notifyAnalysisStarted()
        var librariesScope = this.projectEnvironment.getSearchScopeForProjectLibraries()
        val incrementalCompilationScope = createIncrementalCompilationScope(
            configuration,
            projectEnvironment,
            null,
        )?.also { librariesScope -= it }
        val allSources = mutableListOf<KtSourceFile>().apply {
            addAll(groupedSources.commonSources)
            addAll(groupedSources.platformSources)
        }
        val sessionsWithSources = prepareJvmSessions(
            allSources,
            targetDescription,
            friendPaths = chunk.fold(emptyList()) { paths, m -> paths + m.getFriendPaths() },
            librariesScope,
            isCommonSource = groupedSources.isCommonSourceForLt,
            isScript = { false },
            fileBelongsToModule = groupedSources.fileBelongsToModuleForLt,
            createProviderAndScopeForIncrementalCompilation = { files ->
                val scope = projectEnvironment.getSearchScopeBySourceFiles(files)
                createContextForIncrementalCompilation(
                    compilerConfiguration,
                    projectEnvironment,
                    scope,
                    emptyList(),
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
        FirResult(outputs)
    }
    if (diagnosticsReporter.hasErrors) {
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
            diagnosticsReporter, messageCollector,
            compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        )
        return false
    }
    // K2/LT: FIR2IR
    val fir2IrExtensions = JvmFir2IrExtensions(compilerConfiguration, JvmIrDeserializerImpl())
    val irGenerationExtensions = IrGenerationExtension.getInstances(project)
    val fir2IrAndIrActualizerResult =
        firResult.convertToIrAndActualizeForJvm(fir2IrExtensions, compilerConfiguration, diagnosticsReporter, irGenerationExtensions)
    val factory = JvmIrCodegenFactory(compilerConfiguration)
    val input = fir2IrAndIrActualizerResult.toBackendInput(compilerConfiguration)

    val backendInputForMultiModuleChunk = BackendInputForMultiModuleChunk(
        factory,
        input,
        fir2IrAndIrActualizerResult.irModuleFragment.descriptor,
        NoScopeRecordCliBindingTrace(project).bindingContext,
        FirJvmBackendClassResolver(fir2IrAndIrActualizerResult.components),
        FirJvmBackendExtension(
            fir2IrAndIrActualizerResult.components,
            fir2IrAndIrActualizerResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
        )
    )

    val firFiles = firResult.outputs.flatMap { it.fir }
    if (!checkKotlinPackageUsageForLightTree(compilerConfiguration, firFiles)) return false

    return backendInputForMultiModuleChunk.runBackend(
        project,
        chunk,
        compilerConfiguration,
        messageCollector,
        diagnosticsReporter,
        buildFile,
        allSourceFiles = null,
    )
}

private fun compileSingleModuleUsingFrontendIrAndLightTree(
    project: Project,
    projectEnvironment: VfsBasedProjectEnvironment,
    compilerConfiguration: CompilerConfiguration,
    messageCollector: MessageCollector,
    buildFile: File?,
    module: Module,
    groupedSources: GroupedKtSources,
): Boolean {
    val moduleConfiguration = compilerConfiguration.applyModuleProperties(module, buildFile)

    val context = FrontendContextForSingleModuleLightTree(
        module,
        groupedSources,
        projectEnvironment,
        messageCollector,
        moduleConfiguration
    )

    val (firResult, generationState) = context.compileModule() ?: return false

    val mainClassFqName = runIf(moduleConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
        findMainClass(firResult.outputs.last().fir)
    }

    return writeOutputsIfNeeded(
        project,
        compilerConfiguration,
        messageCollector,
        listOf(generationState),
        mainClassFqName
    )
}

private fun FrontendContextForSingleModuleLightTree.compileModule(): Pair<FirResult, GenerationState>? {
    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    val diagnosticsReporter = createPendingReporter(messageCollector)
    val firResult = compileModuleToAnalyzedFirViaLightTree(
        ModuleCompilerInput(TargetId(module), groupedSources, configuration),
        diagnosticsReporter,
        module.getFriendPaths(),
    )
    if (diagnosticsReporter.hasErrors) {
        diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)
        return null
    }

    if (!checkKotlinPackageUsageForLightTree(configuration, firResult.outputs.flatMap { it.fir })) {
        return null
    }

    val generationState = runBackend(firResult, diagnosticsReporter) ?: return null
    return firResult to generationState
}

private fun FrontendContext.compileModuleToAnalyzedFirViaLightTree(
    input: ModuleCompilerInput,
    diagnosticsReporter: BaseDiagnosticsCollector,
    friendPaths: List<String>,
): FirResult = compileModuleToAnalyzedFirViaLightTreeIncrementally(
    input, diagnosticsReporter, emptyList(), null, friendPaths
)

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

internal fun FrontendContext.compileModuleToAnalyzedFirViaLightTreeIncrementally(
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
