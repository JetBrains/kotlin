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
import org.jetbrains.kotlin.cli.jvm.compiler.FirKotlinToJvmBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.BackendInputForMultiModuleChunk
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.codegenFactoryWithJvmIrBackendInput
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.runCodegen
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.runLowerings
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.applyModuleProperties
import org.jetbrains.kotlin.cli.jvm.compiler.createLibraryListForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.findMainClass
import org.jetbrains.kotlin.cli.jvm.compiler.writeOutputsIfNeeded
import org.jetbrains.kotlin.codegen.CodegenFactory
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
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
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
    val frontendContext = FrontendContextForMultiChunkMode(
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
        val libraryList = createLibraryListForJvm(
            targetDescription, configuration, chunk.fold(emptyList()) { paths, m -> paths + m.getFriendPaths() }
        )
        val allSources = mutableListOf<KtSourceFile>().apply {
            addAll(groupedSources.commonSources)
            addAll(groupedSources.platformSources)
        }
        val sessionsWithSources = prepareJvmSessions(
            allSources, configuration, this.projectEnvironment, Name.special("<$targetDescription>"),
            extensionRegistrars, librariesScope, libraryList,
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
    val (factory, input) = fir2IrAndIrActualizerResult.codegenFactoryWithJvmIrBackendInput(compilerConfiguration)

    val (codegenFactory, wholeBackendInput, moduleDescriptor, bindingContext, firJvmBackendResolver, firJvmBackendExtension, mainClassFqName) = BackendInputForMultiModuleChunk(
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

    /**
     * This part is similar to [org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.compileModules],
     * see "K1/K2 common multi-chunk part"
     */
    val codegenInputs = ArrayList<CodegenFactory.CodegenInput>(chunk.size)
    val firFiles = firResult.outputs.flatMap { it.fir }
    if (!checkKotlinPackageUsageForLightTree(compilerConfiguration, firFiles)) return false
    for (module in chunk) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        val moduleConfiguration = compilerConfiguration.applyModuleProperties(module, buildFile)
        val wholeModule = (wholeBackendInput as JvmIrCodegenFactory.JvmIrBackendInput).irModuleFragment
        val moduleCopy = IrModuleFragmentImpl(wholeModule.descriptor, wholeModule.irBuiltins)
        wholeModule.files.filterTo(moduleCopy.files) { file ->
            file.fileEntry.name in module.getSourceFiles()
        }
        val backendInput = wholeBackendInput.copy(moduleCopy).let {
            if (firJvmBackendExtension != null) {
                it.copy(backendExtension = firJvmBackendExtension)
            } else it
        }
        // Lowerings (per module)
        codegenInputs += runLowerings(
            project, moduleConfiguration, moduleDescriptor, bindingContext,
            sourceFiles = null, module, codegenFactory, backendInput, diagnosticsReporter,
            firJvmBackendResolver
        )
    }

    val outputs = ArrayList<GenerationState>(chunk.size)

    for (input in codegenInputs) {
        // Codegen (per module)
        outputs += runCodegen(input, input.state, codegenFactory, diagnosticsReporter, compilerConfiguration)
    }

    diagnosticsReporter.reportToMessageCollector(
        messageCollector, compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    )

    return writeOutputsIfNeeded(project, compilerConfiguration, messageCollector, outputs, mainClassFqName)
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
    val moduleConfiguration = compilerConfiguration.copy().applyModuleProperties(module, buildFile).apply {
        put(JVMConfigurationKeys.FRIEND_PATHS, module.getFriendPaths())
    }
    val renderDiagnosticNames = moduleConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    val diagnosticsReporter = FirKotlinToJvmBytecodeCompiler.createPendingReporter(messageCollector)

    val firResult = compileModuleToAnalyzedFirViaLightTree(
        ModuleCompilerInput(TargetId(module), groupedSources, moduleConfiguration),
        projectEnvironment,
        diagnosticsReporter,
    )

    if (!checkKotlinPackageUsageForLightTree(moduleConfiguration, firResult.outputs.flatMap { it.fir })) {
        return false
    }

    val mainClassFqName = runIf(moduleConfiguration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
        findMainClass(firResult.outputs.last().fir)
    }

    if (diagnosticsReporter.hasErrors) {
        diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticNames)
        return false
    }

    val compilerEnvironment = ModuleCompilerEnvironment(projectEnvironment, diagnosticsReporter)
    val irInput = convertAnalyzedFirToIr(moduleConfiguration, TargetId(module), firResult, compilerEnvironment)

    val codegenOutput = generateCodeFromIr(irInput, compilerEnvironment)

    diagnosticsReporter.reportToMessageCollector(
        messageCollector, moduleConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    )

    return writeOutputsIfNeeded(
        project,
        compilerConfiguration,
        messageCollector,
        listOf(codegenOutput.generationState),
        mainClassFqName
    )
}

private fun compileModuleToAnalyzedFirViaLightTree(
    input: ModuleCompilerInput,
    projectEnvironment: VfsBasedProjectEnvironment,
    diagnosticsReporter: BaseDiagnosticsCollector,
): FirResult = compileModuleToAnalyzedFirViaLightTreeIncrementally(
    input, projectEnvironment, emptyList(), null, diagnosticsReporter
)

fun compileModuleToAnalyzedFirViaLightTreeIncrementally(
    input: ModuleCompilerInput,
    projectEnvironment: VfsBasedProjectEnvironment,
    previousStepsSymbolProviders: List<FirSymbolProvider>,
    incrementalExcludesScope: AbstractProjectFileSearchScope?,
    diagnosticsReporter: BaseDiagnosticsCollector,
): FirResult {
    val moduleConfiguration = input.configuration
    val performanceManager = moduleConfiguration[CLIConfigurationKeys.PERF_MANAGER]
    performanceManager?.notifyAnalysisStarted()

    var librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
    val rootModuleName = input.targetId.name

    val incrementalCompilationScope = createIncrementalCompilationScope(
        moduleConfiguration,
        projectEnvironment,
        incrementalExcludesScope
    )?.also { librariesScope -= it }

    val extensionRegistrars = FirExtensionRegistrar.getInstances(projectEnvironment.project)

    val allSources = mutableListOf<KtSourceFile>().apply {
        addAll(input.groupedSources.commonSources)
        addAll(input.groupedSources.platformSources)
    }
    // TODO: handle friends paths
    val libraryList = createLibraryListForJvm(rootModuleName, moduleConfiguration, friendPaths = emptyList())
    val sessionsWithSources = prepareJvmSessions(
        allSources, moduleConfiguration, projectEnvironment, Name.special("<$rootModuleName>"),
        extensionRegistrars, librariesScope, libraryList,
        isCommonSource = input.groupedSources.isCommonSourceForLt,
        isScript = { false },
        fileBelongsToModule = input.groupedSources.fileBelongsToModuleForLt,
        createProviderAndScopeForIncrementalCompilation = { files ->
            val scope = projectEnvironment.getSearchScopeBySourceFiles(files)
            createContextForIncrementalCompilation(
                moduleConfiguration,
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
