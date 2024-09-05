/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.pipeline

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.prepareJvmSessions
import org.jetbrains.kotlin.cli.jvm.compiler.FirKotlinToJvmBytecodeCompiler.createPendingReporter
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.BackendInputForMultiModuleChunk
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.codegenFactoryWithJvmIrBackendInput
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.applyModuleProperties
import org.jetbrains.kotlin.cli.jvm.compiler.createContextForIncrementalCompilation
import org.jetbrains.kotlin.cli.jvm.compiler.createLibraryListForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.findMainClass
import org.jetbrains.kotlin.cli.jvm.compiler.writeOutputsIfNeeded
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
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.hmppModuleName
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import org.jetbrains.kotlin.utils.fileUtils.descendantRelativeTo
import java.io.File
import kotlin.collections.map

fun compileModulesUsingFrontendIRAndPsi(
    projectEnvironment: VfsBasedProjectEnvironment,
    compilerConfiguration: CompilerConfiguration,
    messageCollector: MessageCollector,
    allSources: List<KtFile>,
    buildFile: File?,
    module: Module,
): Boolean {
    val targetIds = compilerConfiguration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)
    val incrementalComponents = compilerConfiguration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)

    val project = projectEnvironment.project
    FirAnalysisHandlerExtension.analyze(project, compilerConfiguration)?.let { return it }

    val moduleConfiguration = compilerConfiguration.applyModuleProperties(module, buildFile)
    val context = CompilationContext(
        module,
        allSources,
        projectEnvironment,
        messageCollector,
        moduleConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME),
        moduleConfiguration,
        targetIds,
        incrementalComponents,
        extensionRegistrars = FirExtensionRegistrar.getInstances(project),
        irGenerationExtensions = IrGenerationExtension.getInstances(project)
    )
    val resultAndGenerationState = context.compileModule() ?: return false

    val mainClassFqName: FqName? = runIf(compilerConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
        findMainClass(resultAndGenerationState.first.outputs.last().fir)
    }

    return writeOutputsIfNeeded(
        project,
        compilerConfiguration,
        messageCollector,
        listOf(resultAndGenerationState.second),
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
    val frontendContext = FrontendContextForMultiChunkMode(
        projectEnvironment, environment, compilerConfiguration, project
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
        val (factory, input) = fir2IrAndIrActualizerResult.codegenFactoryWithJvmIrBackendInput(configuration)
        return BackendInputForMultiModuleChunk(
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
    }
}

private fun CompilationContext.compileModule(): Pair<FirResult, GenerationState>? {
    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    if (!checkKotlinPackageUsageForPsi(configuration, allSources)) return null

    val diagnosticsReporter = createPendingReporter(messageCollector)

    val firResult = compileSourceFilesToAnalyzedFirViaPsi(allSources, diagnosticsReporter, module.getModuleName(), module.getFriendPaths())
    if (firResult == null) {
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticName)
        return null
    }

    val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl())
    val fir2IrAndIrActualizerResult =
        firResult.convertToIrAndActualizeForJvm(fir2IrExtensions, configuration, diagnosticsReporter, irGenerationExtensions)

    val generationState = runBackend(
        fir2IrExtensions,
        fir2IrAndIrActualizerResult,
        diagnosticsReporter
    )

    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticName)

    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    return firResult to generationState
}

fun FrontendContext.compileSourceFilesToAnalyzedFirViaPsi(
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

    val sourceScope = projectEnvironment.getSearchScopeByPsiFiles(ktFiles) + projectEnvironment.getSearchScopeForProjectJavaSources()

    var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()

    val providerAndScopeForIncrementalCompilation = createContextForIncrementalCompilation(
        projectEnvironment,
        incrementalComponents,
        configuration,
        targetIds,
        sourceScope
    )

    providerAndScopeForIncrementalCompilation?.precompiledBinariesFileScope?.let {
        librariesScope -= it
    }
    val libraryList = createLibraryListForJvm(rootModuleName, configuration, friendPaths)
    val sessionsWithSources = prepareJvmSessions(
        ktFiles, configuration, projectEnvironment, Name.special("<$rootModuleName>"),
        extensionRegistrars, librariesScope, libraryList,
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

private fun CompilationContext.runBackend(
    fir2IrExtensions: JvmFir2IrExtensions,
    fir2IrActualizedResult: Fir2IrActualizedResult,
    diagnosticsReporter: BaseDiagnosticsCollector,
): GenerationState {
    val (moduleFragment, components, pluginContext, irActualizedResult, _, symbolTable) = fir2IrActualizedResult
    val irInput = ModuleCompilerIrBackendInput(
        TargetId(module),
        configuration,
        fir2IrExtensions,
        moduleFragment,
        components,
        pluginContext,
        irActualizedResult,
        symbolTable
    )

    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    val generationState = generateCodeFromIr(
        irInput, ModuleCompilerEnvironment(projectEnvironment, diagnosticsReporter)
    ).generationState

    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    AnalyzerWithCompilerReport.reportDiagnostics(
        generationState.collectedExtraJvmDiagnostics,
        messageCollector,
        renderDiagnosticName
    )

    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
    return generationState
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
