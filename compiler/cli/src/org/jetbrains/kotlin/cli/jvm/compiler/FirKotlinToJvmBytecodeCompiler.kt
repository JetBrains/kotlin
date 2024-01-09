/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.ModuleCompilerEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.ModuleCompilerIrBackendInput
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.generateCodeFromIr
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.PendingDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isArrayType
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.multiplatform.hmppModuleName
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.runUnless
import java.io.File

object FirKotlinToJvmBytecodeCompiler {
    fun checkNotSupportedPlugins(
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
    ): Boolean {
        val notSupportedPlugins = mutableListOf<String?>().apply {
            compilerConfiguration.get(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS)
                .collectIncompatiblePluginNamesTo(this, ComponentRegistrar::supportsK2)
            compilerConfiguration.get(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)
                .collectIncompatiblePluginNamesTo(this, CompilerPluginRegistrar::supportsK2)
        }

        if (notSupportedPlugins.isNotEmpty()) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                """
                    |There are some plugins incompatible with language version 2.0:
                    |${notSupportedPlugins.joinToString(separator = "\n|") { "  $it" }}
                    |Please use language version 1.9 or below
                """.trimMargin()
            )
            return false
        }

        return true
    }

    fun compileModulesUsingFrontendIRAndPsi(
        projectEnvironment: VfsBasedProjectEnvironment,
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector,
        allSources: List<KtFile>,
        buildFile: File?,
        module: Module,
    ): Boolean {
        val performanceManager = compilerConfiguration.get(CLIConfigurationKeys.PERF_MANAGER)

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
            performanceManager,
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

    private fun <T : Any> List<T>?.collectIncompatiblePluginNamesTo(
        destination: MutableList<String?>,
        supportsK2: T.() -> Boolean
    ) {
        this?.filter { !it.supportsK2() && it::class.java.canonicalName != CLICompiler.SCRIPT_PLUGIN_REGISTRAR_NAME }
            ?.mapTo(destination) { it::class.qualifiedName }
    }

    private fun CompilationContext.compileModule(): Pair<FirResult, GenerationState>? {
        performanceManager?.notifyAnalysisStarted()
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        if (!checkKotlinPackageUsageForPsi(configuration, allSources)) return null

        val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
        val diagnosticsReporter = createPendingReporter(messageCollector)

        val firResult = runFrontend(allSources, diagnosticsReporter, module.getModuleName(), module.getFriendPaths()).also {
            performanceManager?.notifyAnalysisFinished()
        }
        if (firResult == null) {
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
            return null
        }

        performanceManager?.notifyGenerationStarted()
        performanceManager?.notifyIRTranslationStarted()

        val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl(), JvmIrMangler)
        val fir2IrAndIrActualizerResult =
            firResult.convertToIrAndActualizeForJvm(fir2IrExtensions, configuration, diagnosticsReporter, irGenerationExtensions)

        performanceManager?.notifyIRTranslationFinished()

        val generationState = runBackend(
            fir2IrExtensions,
            fir2IrAndIrActualizerResult,
            diagnosticsReporter
        )

        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)

        performanceManager?.notifyIRGenerationFinished()
        performanceManager?.notifyGenerationFinished()
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        return firResult to generationState
    }

    fun createPendingReporter(messageCollector: MessageCollector): PendingDiagnosticsCollectorWithSuppress =
        DiagnosticReporterFactory.createPendingReporter { isError, message ->
            messageCollector.report(if (isError) CompilerMessageSeverity.ERROR else CompilerMessageSeverity.WARNING, message)
        }

    fun FrontendContext.runFrontend(
        ktFiles: List<KtFile>,
        diagnosticsReporter: BaseDiagnosticsCollector,
        rootModuleName: String,
        friendPaths: List<String>,
    ): FirResult? {
        val syntaxErrors = ktFiles.fold(false) { errorsFound, ktFile ->
            AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, messageCollector).isHasErrors or errorsFound
        }

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
            fileBelongsToModule = { file, moduleName -> file.hmppModuleName == moduleName },
            createProviderAndScopeForIncrementalCompilation = { providerAndScopeForIncrementalCompilation }
        )

        val outputs = sessionsWithSources.map { (session, sources) ->
            buildResolveAndCheckFirFromKtFiles(session, sources, diagnosticsReporter)
        }
        outputs.runPlatformCheckers(diagnosticsReporter)

        return runUnless(syntaxErrors || diagnosticsReporter.hasErrors) { FirResult(outputs) }
    }

    private fun CompilationContext.runBackend(
        fir2IrExtensions: JvmFir2IrExtensions,
        fir2IrActualizedResult: Fir2IrActualizedResult,
        diagnosticsReporter: BaseDiagnosticsCollector,
    ): GenerationState {
        val (moduleFragment, components, pluginContext, irActualizedResult) = fir2IrActualizedResult
        val irInput = ModuleCompilerIrBackendInput(
            TargetId(module),
            configuration,
            fir2IrExtensions,
            moduleFragment,
            components,
            pluginContext,
            irActualizedResult
        )

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val generationState = generateCodeFromIr(
            irInput, ModuleCompilerEnvironment(projectEnvironment, diagnosticsReporter), performanceManager
        ).generationState

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        AnalyzerWithCompilerReport.reportDiagnostics(
            FilteredJvmDiagnostics(
                generationState.collectedExtraJvmDiagnostics,
                Diagnostics.EMPTY
            ),
            messageCollector,
            renderDiagnosticName
        )

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        return generationState
    }

    private class CompilationContext(
        val module: Module,
        val allSources: List<KtFile>,
        override val projectEnvironment: VfsBasedProjectEnvironment,
        override val messageCollector: MessageCollector,
        val renderDiagnosticName: Boolean,
        override val configuration: CompilerConfiguration,
        val performanceManager: CommonCompilerPerformanceManager?,
        override val targetIds: List<TargetId>?,
        override val incrementalComponents: IncrementalCompilationComponents?,
        override val extensionRegistrars: List<FirExtensionRegistrar>,
        val irGenerationExtensions: Collection<IrGenerationExtension>
    ) : FrontendContext

    class FrontendContextForMultiChunkMode private constructor(
        override val projectEnvironment: VfsBasedProjectEnvironment,
        override val messageCollector: MessageCollector,
        override val incrementalComponents: IncrementalCompilationComponents?,
        override val extensionRegistrars: List<FirExtensionRegistrar>,
        override val configuration: CompilerConfiguration,
        override val targetIds: List<TargetId>?
    ) : FrontendContext {
        constructor(
            projectEnvironment: VfsBasedProjectEnvironment,
            environment: KotlinCoreEnvironment,
            compilerConfiguration: CompilerConfiguration,
            project: Project?
        ) : this(
            projectEnvironment,
            environment.messageCollector,
            incrementalComponents = compilerConfiguration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS),
            extensionRegistrars = project?.let { FirExtensionRegistrar.getInstances(it) } ?: emptyList(),
            configuration = compilerConfiguration,
            targetIds = compilerConfiguration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)
        )
    }

    interface FrontendContext {
        val projectEnvironment: VfsBasedProjectEnvironment
        val messageCollector: MessageCollector
        val incrementalComponents: IncrementalCompilationComponents?
        val extensionRegistrars: List<FirExtensionRegistrar>
        val configuration: CompilerConfiguration
        val targetIds: List<TargetId>?
    }
}

fun findMainClass(fir: List<FirFile>): FqName? {
    // TODO: replace with proper main function detector, KT-44557
    val compatibleClasses = mutableListOf<FqName>()
    val visitor = object : FirVisitorVoid() {
        lateinit var file: FirFile

        override fun visitElement(element: FirElement) {}

        override fun visitFile(file: FirFile) {
            this.file = file
            file.acceptChildren(this)
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
            if (simpleFunction.name.asString() != "main") return
            if (simpleFunction.typeParameters.isNotEmpty()) return
            when (simpleFunction.valueParameters.size) {
                0 -> {}
                1 -> {
                    val parameterType = simpleFunction.valueParameters.single().returnTypeRef.coneType
                    if (!parameterType.isArrayType || parameterType.arrayElementType()?.isString != true) return
                }
                else -> return
            }

            compatibleClasses += FqName.fromSegments(
                file.packageFqName.pathSegments().map { it.asString() } + "${file.name.removeSuffix(".kt").capitalize()}Kt"
            )
        }
    }
    fir.forEach { it.accept(visitor) }
    return compatibleClasses.singleOrNull()
}
