/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.pipeline

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.prepareJvmSessions
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.FirKotlinToJvmBytecodeCompiler.createPendingReporter
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.BackendInputForMultiModuleChunk
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.toBackendInput
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.hmppModuleName
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.fileUtils.descendantRelativeTo
import java.io.File

fun compileSingleModuleUsingFrontendIrAndPsi(
    project: Project,
    projectEnvironment: VfsBasedProjectEnvironment,
    compilerConfiguration: CompilerConfiguration,
    messageCollector: MessageCollector,
    buildFile: File?,
    module: Module,
    allSources: List<KtFile>,
): Boolean {
    FirAnalysisHandlerExtension.analyze(project, compilerConfiguration)?.let { return it }

    val moduleConfiguration = compilerConfiguration.applyModuleProperties(module, buildFile)
    val context = FrontendContextForSingleModulePsi(
        module,
        allSources,
        projectEnvironment,
        messageCollector,
        moduleConfiguration
    )
    val (firResult, generationState) = context.compileModule() ?: return false

    val mainClassFqName: FqName? = runIf(compilerConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
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

internal fun runFrontendAndGenerateIrForMultiModuleChunkUsingFrontendIRAndPsi(
    environment: KotlinCoreEnvironment,
    projectEnvironment: VfsBasedProjectEnvironment,
    compilerConfiguration: CompilerConfiguration,
    chunk: List<Module>,
): BackendInputForMultiModuleChunk? {
    val sourceFiles = environment.getSourceFiles()
    val project = projectEnvironment.project
    val messageCollector = environment.configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)
    val frontendContext = createFrontendContextForMultiChunkMode(
        projectEnvironment, messageCollector, compilerConfiguration, project
    )

    with(frontendContext) {
        // K2/PSI: frontend
        val firResult = compileSourceFilesToAnalyzedFirViaPsi(
            sourceFiles, diagnosticsReporter, chunk.joinToString(separator = "+") { it.getModuleName() },
            chunk.fold(emptyList()) { paths, m -> paths + m.getFriendPaths() }
        ) ?: run {
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
                diagnosticsReporter, messageCollector,
                compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
            )
            return null
        }
        // K2/PSI: FIR2IR
        val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl())
        val irGenerationExtensions = IrGenerationExtension.getInstances(project)
        val fir2IrAndIrActualizerResult =
            firResult.convertToIrAndActualizeForJvm(fir2IrExtensions, configuration, diagnosticsReporter, irGenerationExtensions)
        val factory = JvmIrCodegenFactory(configuration)
        val input = fir2IrAndIrActualizerResult.toBackendInput(configuration)
        return BackendInputForMultiModuleChunk(
            factory,
            input,
            fir2IrAndIrActualizerResult.irModuleFragment.descriptor,
            FirJvmBackendClassResolver(fir2IrAndIrActualizerResult.components),
            FirJvmBackendExtension(
                fir2IrAndIrActualizerResult.components,
                fir2IrAndIrActualizerResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
            )
        )
    }
}

private fun FrontendContextForSingleModulePsi.compileModule(): Pair<FirResult, GenerationState>? {
    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    val diagnosticsReporter = createPendingReporter(messageCollector)
    val firResult = compileSourceFilesToAnalyzedFirViaPsi(allSources, diagnosticsReporter, module.getModuleName(), module.getFriendPaths())
    if (firResult == null) {
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticName)
        return null
    }

    if (!checkKotlinPackageUsageForPsi(configuration, allSources)) {
        return null
    }

    val generationState = runBackend(firResult, diagnosticsReporter) ?: return null
    return firResult to generationState
}

internal fun FrontendContext.compileSourceFilesToAnalyzedFirViaPsi(
    ktFiles: List<KtFile>,
    diagnosticsReporter: BaseDiagnosticsCollector,
    rootModuleName: String,
    friendPaths: List<String>,
    ignoreErrors: Boolean = false,
): FirResult? {
    val performanceManager = configuration.get(CLIConfigurationKeys.PERF_MANAGER)
    performanceManager?.notifyAnalysisStarted()

    val syntaxErrors = ktFiles.fold(false) { errorsFound, ktFile ->
        AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, messageCollector).isHasErrors or errorsFound
    }

    val scriptsInCommonSourcesErrors = reportCommonScriptsError(ktFiles)

    val sourceScope = (projectEnvironment as VfsBasedProjectEnvironment).getSearchScopeByPsiFiles(ktFiles) +
            projectEnvironment.getSearchScopeForProjectJavaSources()

    var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()

    val providerAndScopeForIncrementalCompilation = createContextForIncrementalCompilation(projectEnvironment, configuration, sourceScope)

    providerAndScopeForIncrementalCompilation?.precompiledBinariesFileScope?.let {
        librariesScope -= it
    }
    val sessionsWithSources = prepareJvmSessions(
        ktFiles,
        rootModuleName,
        friendPaths,
        librariesScope,
        isCommonSource = { it.isCommonSource == true },
        isScript = { it.isScript() },
        fileBelongsToModule = { file, moduleName -> file.hmppModuleName == moduleName },
        createProviderAndScopeForIncrementalCompilation = { providerAndScopeForIncrementalCompilation }
    )

    val outputs = sessionsWithSources.map { (session, sources) ->
        buildResolveAndCheckFirFromKtFiles(session, sources, diagnosticsReporter)
    }
    outputs.runPlatformCheckers(diagnosticsReporter)

    performanceManager?.notifyAnalysisFinished()
    return runUnless(!ignoreErrors && (syntaxErrors || scriptsInCommonSourcesErrors || diagnosticsReporter.hasErrors)) { FirResult(outputs) }
}

private fun FrontendContext.reportCommonScriptsError(ktFiles: List<KtFile>): Boolean {
    val lastHmppModule = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)?.modules?.lastOrNull()
    val commonScripts = ktFiles.filter { it.isScript() && (it.isCommonSource == true || it.hmppModuleName != lastHmppModule?.name) }
    if (commonScripts.isNotEmpty()) {
        val cwd = File(".").absoluteFile
        fun renderFile(ktFile: KtFile) = File(ktFile.virtualFilePath).descendantRelativeTo(cwd).path
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            "Script files in common source roots are not supported. Misplaced files:\n    " +
                    commonScripts.joinToString("\n    ", transform = ::renderFile)
        )
        return true
    }
    return false
}
