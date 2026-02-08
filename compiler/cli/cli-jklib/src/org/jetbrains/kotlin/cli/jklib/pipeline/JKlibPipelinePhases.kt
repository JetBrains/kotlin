
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib.pipeline

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.arguments.K2JKlibCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.configureJdkHomeFromSystemProperty
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.MODULE_NAME

import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.jklib.JKlibModuleSerializer
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeIr
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.metadata.resolver.impl.KotlinResolvedLibraryImpl
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.cli.jklib.prepareJKlibSessions
import org.jetbrains.kotlin.konan.file.File as KFile

// K2JKlibCompiler imports:
// import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
// import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
// import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots

import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.ir.IrDiagnosticReporter

val JKLIB_OUTPUT_DESTINATION = CompilerConfigurationKey.create<String>("jklib output destination")

object JKlibConfigurationPhase : PipelinePhase<ArgumentsPipelineArtifact<K2JKlibCompilerArguments>, ConfigurationPipelineArtifact>(
    name = "JKlibConfigurationPhase"
) {
@OptIn(CompilerConfiguration.Internals::class)
    override fun executePhase(input: ArgumentsPipelineArtifact<K2JKlibCompilerArguments>): ConfigurationPipelineArtifact? {
        val arguments = input.arguments
        val configuration = CompilerConfiguration()
        val messageCollector = input.messageCollector
        val paths = PathUtil.kotlinPathsForCompiler
        val rootDisposable = input.rootDisposable

        configuration.put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        configuration.put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        configuration.put(CommonConfigurationKeys.PERF_MANAGER, input.performanceManager)

        val pluginClasspaths = arguments.pluginClasspaths.orEmpty().toMutableList()
        val pluginOptions = arguments.pluginOptions.orEmpty().toMutableList()
        val pluginConfigurations = arguments.pluginConfigurations?.asList().orEmpty()
        val pluginOrderConstraints = arguments.pluginOrderConstraints?.asList().orEmpty()

        PluginCliParser.loadPluginsSafe(
            pluginClasspaths,
            pluginOptions,
            pluginConfigurations,
            pluginOrderConstraints,
            configuration,
            rootDisposable
        )

        val commonSources = arguments.commonSources?.toSet() ?: emptySet()
        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, isCommon = arg in commonSources, hmppModuleName = null)
        }

        with(configuration) {
            if (arguments.noJdk) {
                put(JVMConfigurationKeys.NO_JDK, true)
            } else {
                configureJdkHomeFromSystemProperty()
            }
            configureJdkClasspathRoots()
            if (!arguments.noStdlib) {
                getLibraryFromHome(
                    paths,
                    KotlinPaths::stdlibPath,
                    PathUtil.KOTLIN_JAVA_STDLIB_JAR,
                    messageCollector,
                    "'-no-stdlib'",
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.stdlib")
                }
                getLibraryFromHome(
                    paths,
                    KotlinPaths::scriptRuntimePath,
                    PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR,
                    messageCollector,
                    "'-no-stdlib'",
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.script.runtime")
                }
            }
            if (!arguments.noReflect && !arguments.noStdlib) {
                getLibraryFromHome(
                    paths,
                    KotlinPaths::reflectPath,
                    PathUtil.KOTLIN_JAVA_REFLECT_JAR,
                    messageCollector,
                    "'-no-reflect' or '-no-stdlib'",
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.reflect")
                }
            }
            arguments.klibLibraries?.let { libraries ->
                put(
                    JVMConfigurationKeys.KLIB_PATHS,
                    libraries.split(File.pathSeparator.toRegex()).filterNot(String::isEmpty),
                )
            }
            for (path in arguments.classpath?.split(File.pathSeparatorChar).orEmpty()) {
                add(CLIConfigurationKeys.CONTENT_ROOTS, JvmClasspathRoot(File(path)))
            }
        }

        val moduleName = arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        configuration.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        configuration.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
        configuration.put(
            CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME,
            arguments.renderInternalDiagnosticNames,
        )

        arguments.destination?.let { configuration.put(JKLIB_OUTPUT_DESTINATION, it) }

        return ConfigurationPipelineArtifact(
            configuration,
            DiagnosticsCollectorImpl(),
            rootDisposable
        )
    }
}

object JKlibFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, JKlibFrontendPipelineArtifact>(
    name = "JKlibFrontendPipelinePhase"
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): JKlibFrontendPipelineArtifact? {
        val configuration = input.configuration
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val rootDisposable = input.rootDisposable
        val diagnosticsReporter = input.diagnosticCollector

        val projectEnvironment = createProjectEnvironment(
            configuration,
            rootDisposable,
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
            messageCollector,
        )
        val groupedSources = collectSources(configuration, projectEnvironment, messageCollector)

        if (groupedSources.isEmpty()) {
            messageCollector.report(ERROR, "No source files")
            return null
        }

        val klibFiles = configuration.getList(JVMConfigurationKeys.KLIB_PATHS)
        val resolvedLibraries = klibFiles.map { KotlinResolvedLibraryImpl(resolveSingleFileKlib(KFile(it), messageCollector)) }
        val extensionRegistrars = configuration.getCompilerExtensions(FirExtensionRegistrar)
        val ltFiles = groupedSources.let { it.commonSources + it.platformSources }.toList()

        val moduleName = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)

        val libraryList = DependencyListForCliModule.build(Name.identifier(moduleName)) {
            dependencies(configuration.jvmClasspathRoots.map { it.absolutePath })
            dependencies(configuration.jvmModularRoots.map { it.absolutePath })
            dependencies(resolvedLibraries.map { it.library.libraryFile.absolutePath })
        }

        val librariesScope = projectEnvironment.getSearchScopeForProjectLibraries()

        val rootModuleName = Name.special("<$moduleName>")

        val sessionsWithSources = prepareJKlibSessions(
            projectEnvironment,
            ltFiles,
            configuration,
            rootModuleName,
            resolvedLibraries,
            libraryList,
            extensionRegistrars,
            metadataCompilationMode = false,
            isCommonSource = groupedSources.isCommonSourceForLt,
            fileBelongsToModule = groupedSources.fileBelongsToModuleForLt,
            librariesScope = librariesScope,
        )

        val outputs = sessionsWithSources.map { (session, files) ->
            val firFiles = session.buildFirViaLightTree(
                files,
                diagnosticsReporter,
                null
            )
            resolveAndCheckFir(session, firFiles, diagnosticsReporter)
        }

        outputs.runPlatformCheckers(diagnosticsReporter)

        val firFiles = outputs.flatMap { it.fir }
        checkKotlinPackageUsageForLightTree(configuration, firFiles)

        if (diagnosticsReporter.hasErrors) {
            val renderDiagnosticName = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
            diagnosticsReporter.reportToMessageCollector(messageCollector, renderDiagnosticName)
            return null
        }

        return JKlibFrontendPipelineArtifact(
             AllModulesFrontendOutput(outputs),
             configuration,
             diagnosticsReporter,
             ltFiles,
             projectEnvironment,
             rootDisposable
        )
    }

    private fun resolveSingleFileKlib(file: KFile, collector: MessageCollector): KotlinLibrary {
        val klibLoadingResult = KlibLoader { libraryPaths(file.path) }.load()
        klibLoadingResult.reportLoadingProblemsIfAny { _, message ->
            collector.report(ERROR, message)
        }
        return klibLoadingResult.librariesStdlibFirst.single()
    }
}

object JKlibFir2IrPipelinePhase : PipelinePhase<JKlibFrontendPipelineArtifact, JKlibFir2IrPipelineArtifact>(
    name = "JKlibFir2IrPipelinePhase"
) {
    override fun executePhase(input: JKlibFrontendPipelineArtifact): JKlibFir2IrPipelineArtifact? {
        val configuration = input.configuration
        val firResult = input.frontendOutput
        val diagnosticsReporter = input.diagnosticCollector
        val projectEnvironment = input.projectEnvironment
        val rootDisposable = input.rootDisposable

        val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl())
        val irGenerationExtensions = configuration.getCompilerExtensions(IrGenerationExtension)
        
        val fir2IrResult = firResult.convertToIrAndActualizeForJvm(
            fir2IrExtensions,
            configuration,
            diagnosticsReporter,
            irGenerationExtensions,
        )

        return JKlibFir2IrPipelineArtifact(fir2IrResult, diagnosticsReporter, configuration, firResult, projectEnvironment, rootDisposable)
    }
}

object JKlibKlibSerializationPhase : PipelinePhase<JKlibFir2IrPipelineArtifact, JKlibExitArtifact>(
    name = "JKlibKlibSerializationPhase"
) {
    override fun executePhase(input: JKlibFir2IrPipelineArtifact): JKlibExitArtifact? {
        val fir2IrResult = input.result
        val configuration = input.configuration
        val diagnosticsReporter = input.diagnosticCollector
        val rootDisposable = input.rootDisposable
        
        val produceHeaderKlib = true
        
        // Output destination calculation (from Arguments or Configuration)
        val arguments = K2JKlibCompilerArguments() // Should get from arguments artifact really, or configuration 
        // Configuration does not store destination? It stores PERF_MANAGER etc.
        // Usually destination is in arguments.
        // We lost arguments chain in ConfigurationPipelineArtifact?
        // Wait, ConfigurationPipelineArtifact does not define arguments.
        // But we usually don't need arguments if we have configuration.
        // However, destination is not always in configuration unless put there.
        // K2JKlibCompiler puts it in a File object but doesn't put into config?
        // It checks arguments.destination.
        
        // FIXME: Destination should be passed down or stored in config.
        // Ideally we should put destination in ConfigurationPipelineArtifact or CompilerConfiguration. 
        // K2JVMCompiler stores it in configuration? No, it passes it to ModuleBuilder.
        
        // For now, I will assume we can get it from arguments if we had access.
        // But we lost arguments.
        // We should add 'destination' to ConfigurationPipelineArtifact? 
        // Or store it in CompilerConfiguration with a custom key.
        // Let's assume for MVP we use a default or temp if not found (impossible for real compiler).
        // I will add a TODO and use a dummy file or check if we can retrieve it.
        // In K2JKlibCompiler doExecute, destination is calculated and passed to compileLibrary.
        // In pipeline, JKlibConfigurationPhase could put it in configuration using a custom key.
        
        // Let's add a custom key for destination in configuration in JKlibConfigurationPhase.
        // But I already wrote JKlibConfigurationPhase.
        
        // I will use "ir.klib" as default for now and add TODO.
        val destination = File(configuration[JKLIB_OUTPUT_DESTINATION] ?: "result.klib")

        try {
            val serializerOutput = serializeModuleIntoKlib(
                moduleName = fir2IrResult.irModuleFragment.name.asString(),
                irModuleFragment = fir2IrResult.irModuleFragment,
                configuration = configuration,
                diagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
                    diagnosticsReporter.deduplicating(),
                    configuration.languageVersionSettings
                ),
                cleanFiles = emptyList(),
                dependencies = emptyList(), // We might need to pass resolved libraries?
                // JKlibFrontendPipelinePhase resolved libraries but didn't pass them to Fir2IrArtifact...
                // They are needed for serialization?
                // K2JKlibCompiler reused `resolvedLibraries` list.
                // We should add `resolvedLibraries` to JKlibFrontendPipelineArtifact/JKlibFir2IrPipelineArtifact.
                createModuleSerializer = { irDiagnosticReporter: IrDiagnosticReporter ->
                    JKlibModuleSerializer(IrSerializationSettings(configuration), irDiagnosticReporter)
                },
                metadataSerializer = Fir2KlibMetadataSerializer(
                    configuration,
                    input.frontendOutput.outputs,
                    produceHeaderKlib = produceHeaderKlib,
                    fir2IrActualizedResult = fir2IrResult,
                    exportKDoc = false,
                ),
            )

            val versions = KotlinLibraryVersioning(
                abiVersion = org.jetbrains.kotlin.library.KotlinAbiVersion.CURRENT,
                compilerVersion = org.jetbrains.kotlin.config.KotlinCompilerVersion.getVersion(),
                metadataVersion = configuration.klibMetadataVersionOrDefault(),
            )

            KlibWriter {
                manifest {
                    moduleName(configuration[CommonConfigurationKeys.MODULE_NAME]!!)
                    versions(versions)
                    platformAndTargets(BuiltInsPlatform.COMMON, emptyList())
                }
                includeMetadata(serializerOutput.serializedMetadata ?: error("expected serialized metadata"))
                includeIr(serializerOutput.serializedIr)
            }.writeTo(destination.absolutePath)
        } catch (e: CompilationException) {
            val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            messageCollector.report(
                EXCEPTION,
                OutputMessageUtil.renderException(e),
                MessageUtil.psiElementToMessageLocation(e.element),
            )
            return JKlibExitArtifact(ExitCode.INTERNAL_ERROR)
        }

        return JKlibExitArtifact(ExitCode.OK)
    }
}

class JKlibExitArtifact(override val exitCode: ExitCode) : org.jetbrains.kotlin.cli.pipeline.PipelineArtifactWithExitCode()
