/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib.pipeline

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib
import org.jetbrains.kotlin.cli.CliDiagnostics.CLASSPATH_RESOLUTION_ERROR
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_ARGUMENTS_ERROR
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2JKlibCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.jklib.prepareJKlibSessions
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.config.*
import org.jetbrains.kotlin.cli.jvm.configureJdkHomeFromSystemProperty
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.jklib.JKlibModuleSerializer
import org.jetbrains.kotlin.library.KlibFormat
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.includeIr
import org.jetbrains.kotlin.library.writer.includeMetadata
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File


val JKLIB_OUTPUT_DESTINATION = CompilerConfigurationKey.create<String>("jklib output destination")

var CompilerConfiguration.jklibOutputDestination: String?
    get() = get(JKLIB_OUTPUT_DESTINATION)
    set(value) {
        putIfNotNull(JKLIB_OUTPUT_DESTINATION, value)
    }

object JKlibConfigurationPhase : AbstractConfigurationPhase<K2JKlibCompilerArguments>(
    name = "JKlibConfigurationPhase",
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector),
    configurationUpdaters = listOf(JKlibConfigurationUpdater)
) {
    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return MetadataVersion(*versionArray)
    }
}

object JKlibConfigurationUpdater : ConfigurationUpdater<K2JKlibCompilerArguments>() {
    override fun fillConfiguration(
        input: ArgumentsPipelineArtifact<K2JKlibCompilerArguments>,
        configuration: CompilerConfiguration,
    ) {
        val arguments = input.arguments

        val commonSources = arguments.commonSources.toSet()
        val hmppCliModuleStructure = configuration.hmppModuleStructure
        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(
                path = arg,
                isCommon = arg in commonSources,
                hmppModuleName = hmppCliModuleStructure?.getModuleNameForSource(arg)
            )
        }

        // TODO(KT-84899): Reuse code from K2JVM
        with(configuration) {
            if (arguments.noJdk) {
                put(JVMConfigurationKeys.NO_JDK, true)
            } else {
                configureJdkHomeFromSystemProperty()
            }
            configureJdkClasspathRoots()
            val paths = PathUtil.kotlinPathsForCompiler
            if (!arguments.noStdlib) {
                getLibraryFromHome(
                    paths,
                    KotlinPaths::stdlibPath,
                    PathUtil.KOTLIN_JAVA_STDLIB_JAR,
                    configuration,
                    "'-no-stdlib'",
                )?.let { file ->
                    add(CLIConfigurationKeys.CONTENT_ROOTS, JvmModulePathRoot(file))
                    add(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULES, "kotlin.stdlib")
                }
                getLibraryFromHome(
                    paths,
                    KotlinPaths::scriptRuntimePath,
                    PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR,
                    configuration,
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
                    configuration,
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

            arguments.samConversions?.let {
                val parsedValue = JvmClosureGenerationScheme.fromString(it)
                if (parsedValue != null) {
                    put(JVMConfigurationKeys.SAM_CONVERSIONS, parsedValue)
                } else {
                    val messageCollector = getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
                    messageCollector.report(
                        ERROR,
                        "Unknown `-Xsam-conversions` argument: ${it}\n." +
                                "Supported arguments: ${JvmClosureGenerationScheme.entries.joinToString { scheme -> scheme.description }}"
                    )
                }
            }
        }

        val moduleName = arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        configuration.moduleName = moduleName

        configuration.allowKotlinPackage = arguments.allowKotlinPackage
        configuration.renderDiagnosticInternalName = arguments.renderInternalDiagnosticNames

        arguments.destination?.let { configuration.jklibOutputDestination = it }
        arguments.friendModules?.let { configuration.friendPaths = it.split(File.pathSeparator).filterNot(String::isEmpty) }
    }
}

object JKlibFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, JKlibFrontendPipelineArtifact>(
    name = "JKlibFrontendPipelinePhase",
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): JKlibFrontendPipelineArtifact? {
        val configuration = input.configuration
        val rootDisposable = input.rootDisposable
        val diagnosticsReporter = configuration.diagnosticsCollector

        val projectEnvironment = createProjectEnvironment(
            configuration,
            rootDisposable,
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
        )
        val groupedSources = collectSources(configuration, projectEnvironment)

        if (groupedSources.isEmpty()) {
            configuration.report(COMPILER_ARGUMENTS_ERROR, "No source files")
            return null
        }

        val klibFiles = configuration.klibPaths

        val klibLoadingResult = KlibLoader { libraryPaths(klibFiles) }.load()
        klibLoadingResult.reportLoadingProblemsIfAny { _, message ->
            configuration.report(CLASSPATH_RESOLUTION_ERROR, message)
        }

        val resolvedLibraries = klibLoadingResult.librariesStdlibFirst
        val extensionRegistrars = configuration.getCompilerExtensions(FirExtensionRegistrar)
        val ltFiles = groupedSources.let { it.commonSources + it.platformSources }.toList()

        val moduleName = configuration.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME

        val libraryList = DependencyListForCliModule.build(Name.identifier(moduleName)) {
            dependencies(configuration.jvmClasspathRoots.map { it.absolutePath })
            dependencies(configuration.jvmModularRoots.map { it.absolutePath })
            dependencies(resolvedLibraries.map { it.libraryFile.absolutePath })
            friendDependencies(configuration.friendPaths)
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
                reportFilesAndLines = null
            )
            resolveAndCheckFir(session, firFiles, diagnosticsReporter)
        }

        outputs.runPlatformCheckers(diagnosticsReporter)

        val firFiles = outputs.flatMap { it.fir }
        checkKotlinPackageUsageForLightTree(configuration, firFiles)

        return JKlibFrontendPipelineArtifact(AllModulesFrontendOutput(outputs), configuration)
    }
}

object JKlibFir2IrPipelinePhase : PipelinePhase<JKlibFrontendPipelineArtifact, JKlibFir2IrPipelineArtifact>(
    name = "JKlibFir2IrPipelinePhase",
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: JKlibFrontendPipelineArtifact): JKlibFir2IrPipelineArtifact {
        val configuration = input.configuration
        val firResult = input.frontendOutput
        val diagnosticsReporter = configuration.diagnosticsCollector
        val fir2IrExtensions = JvmFir2IrExtensions(configuration)
        val irGenerationExtensions = configuration.getCompilerExtensions(IrGenerationExtension)

        val fir2IrResult = firResult.convertToIrAndActualizeForJvm(
            fir2IrExtensions,
            configuration,
            diagnosticsReporter,
            irGenerationExtensions,
        )

        return JKlibFir2IrPipelineArtifact(fir2IrResult, configuration, firResult)
    }
}

object JKlibKlibSerializationPhase : PipelinePhase<JKlibFir2IrPipelineArtifact, JKlibExitArtifact>(
    name = "JKlibKlibSerializationPhase",
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: JKlibFir2IrPipelineArtifact): JKlibExitArtifact {
        val fir2IrResult = input.result
        val configuration = input.configuration
        val diagnosticsReporter = configuration.diagnosticsCollector
        val destination = File(configuration.jklibOutputDestination ?: "result.klib")

        val serializerOutput = serializeModuleIntoKlib(
            moduleName = fir2IrResult.irModuleFragment.name.asString(),
            irModuleFragment = fir2IrResult.irModuleFragment,
            configuration = configuration,
            diagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
                diagnosticsReporter.deduplicating(),
                configuration.languageVersionSettings
            ),
            cleanFiles = emptyList(),
            dependencies = emptyList(),
            createModuleSerializer = { irDiagnosticReporter: IrDiagnosticReporter ->
                JKlibModuleSerializer(IrSerializationSettings(configuration), irDiagnosticReporter)
            },
            metadataSerializer = Fir2KlibMetadataSerializer(
                configuration,
                input.frontendOutput.outputs,
                fir2IrActualizedResult = fir2IrResult,
                exportKDoc = false,
                produceHeaderKlib = false,
            ),
        )

        val versions = KotlinLibraryVersioning(
            abiVersion = KotlinAbiVersion.CURRENT,
            compilerVersion = KotlinCompilerVersion.getVersion(),
            metadataVersion = configuration.klibMetadataVersionOrDefault(),
        )

        KlibWriter {
            format(KlibFormat.ZipArchive)
            manifest {
                moduleName(configuration.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME)
                versions(versions)
                platformAndTargets(BuiltInsPlatform.JKLIB, emptyList())
            }
            includeMetadata(serializerOutput.serializedMetadata ?: error("expected serialized metadata"))
            includeIr(serializerOutput.serializedIr)
        }.writeTo(destination.absolutePath)


        return JKlibExitArtifact(ExitCode.OK, configuration)
    }
}

class JKlibExitArtifact(override val exitCode: ExitCode, override val configuration: CompilerConfiguration) :
    PipelineArtifactWithExitCode() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): PipelineArtifact {
        return JKlibExitArtifact(exitCode, newConfiguration)
    }
}
