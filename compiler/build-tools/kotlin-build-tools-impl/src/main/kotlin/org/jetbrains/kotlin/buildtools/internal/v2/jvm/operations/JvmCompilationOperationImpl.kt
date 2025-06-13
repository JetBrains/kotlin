/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2.jvm.operations

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation.Companion.PROJECT_ID
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.api.v2.CommonCompilerArguments.Companion.XUSE_FIR_IC
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.v2.ExecutionPolicy.Companion.DAEMON_JVM_ARGUMENTS
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.BACKUP_CLASSES
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.FORCE_RECOMPILATION
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.KEEP_IC_CACHES_IN_MEMORY
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.MODULE_BUILD_DIR
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.OUTPUT_DIRS
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.PRECISE_JAVA_TRACKING
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.ROOT_PROJECT_DIR
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.v2.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.v2.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptionsImpl
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.isAdvanced
import org.jetbrains.kotlin.cli.common.arguments.resolvedDelimiter
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.storage.FileLocations
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.rmi.RemoteException
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

class JvmCompilationOperationImpl(
    private val kotlinSources: List<Path>,
    private val destinationDirectory: Path,
    override val compilerArguments: JvmCompilerArgumentsImpl = JvmCompilerArgumentsImpl(),
) : BuildOperationImpl<CompilationResult>(), JvmCompilationOperation {

    override fun <V> get(key: JvmCompilationOperation.Option<V>): V = optionsDelegate[key]
    override fun <V> set(key: JvmCompilationOperation.Option<V>, value: V) {
        optionsDelegate[key] = value
    }

    private val buildIdToSessionFlagFile: MutableMap<ProjectId, File> = ConcurrentHashMap()

    override fun createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions {
        return JvmSnapshotBasedIncrementalCompilationOptionsImpl()
    }

    override fun execute(executionPolicy: ExecutionPolicy?, logger: KotlinLogger?): CompilationResult {
        val loggerAdapter =
            logger?.let { KotlinLoggerMessageCollectorAdapter(it) } ?: KotlinLoggerMessageCollectorAdapter(DefaultKotlinLogger)
        return if (executionPolicy == null || executionPolicy[ExecutionPolicy.EXECUTION_MODE] == ExecutionPolicy.ExecutionMode.IN_PROCESS) {
            compileInProcess(loggerAdapter)
        } else if (executionPolicy[ExecutionPolicy.EXECUTION_MODE] == ExecutionPolicy.ExecutionMode.DAEMON) {
            compileWithDaemon(requireNotNull(get(PROJECT_ID)), executionPolicy, loggerAdapter)
        } else {
            CompilationResult.COMPILATION_ERROR.also {
                loggerAdapter.kotlinLogger.error("Unknown execution mode: ${executionPolicy[ExecutionPolicy.EXECUTION_MODE]}")
            }
        }
    }

    private fun JvmCompilerArgumentsImpl.toDaemonCompilationOptions(logger: KotlinLogger): CompilationOptions {
        val ktsExtensionsAsArray = get(JvmCompilationOperation.Companion.KOTLINSCRIPT_EXTENSIONS)
        val reportCategories = arrayOf(
            ReportCategory.COMPILER_MESSAGE.code, ReportCategory.IC_MESSAGE.code
        ) // TODO: automagically compute the value, related to BasicCompilerServicesWithResultsFacadeServer
        val reportSeverity = if (logger.isDebugEnabled) {
            ReportSeverity.DEBUG.code
        } else {
            ReportSeverity.INFO.code
        }
        val aggregatedIcConfiguration: JvmIncrementalCompilationConfiguration? = get(INCREMENTAL_COMPILATION)
        return when (aggregatedIcConfiguration) {
            is JvmSnapshotBasedIncrementalCompilationConfiguration -> {
                val sourcesChanges = aggregatedIcConfiguration.sourcesChanges
                val requestedCompilationResults = arrayOf(
                    CompilationResultCategory.IC_COMPILE_ITERATION.code,
                )
                val classpathChanges = aggregatedIcConfiguration.classpathChanges
                IncrementalCompilationOptions(
                    sourcesChanges,
                    classpathChanges = classpathChanges,
                    workingDir = aggregatedIcConfiguration.workingDirectory.toFile(),
                    compilerMode = CompilerMode.INCREMENTAL_COMPILER,
                    targetPlatform = CompileService.TargetPlatform.JVM,
                    reportCategories = reportCategories,
                    reportSeverity = reportSeverity,
                    requestedCompilationResults = requestedCompilationResults,
                    outputFiles = aggregatedIcConfiguration.options[OUTPUT_DIRS]?.map { it.toFile() },
                    multiModuleICSettings = null, // required only for the build history approach
                    modulesInfo = null, // required only for the build history approach
                    rootProjectDir = aggregatedIcConfiguration.options[ROOT_PROJECT_DIR].toFile(),
                    buildDir = aggregatedIcConfiguration.options[MODULE_BUILD_DIR].toFile(),
                    kotlinScriptExtensions = ktsExtensionsAsArray,
                    icFeatures = aggregatedIcConfiguration.extractIncrementalCompilationFeatures(),
                    useJvmFirRunner = aggregatedIcConfiguration.options[USE_FIR_RUNNER],
                )
            }
            // no IC configuration -> non-incremental compilation
            null -> CompilationOptions(
                compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                targetPlatform = CompileService.TargetPlatform.JVM,
                reportCategories = reportCategories,
                reportSeverity = reportSeverity,
                requestedCompilationResults = emptyArray(),
                kotlinScriptExtensions = ktsExtensionsAsArray,
            )
            else -> error(
                "Unexpected incremental compilation configuration: $aggregatedIcConfiguration. " + "In this version, it must be an instance of JvmIncrementalCompilationConfiguration " + "for incremental compilation, or null for non-incremental compilation."
            )
        }
    }


    private fun compileWithDaemon(
        projectId: ProjectId,
        executionPolicy: ExecutionPolicy,
        loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    ): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling using the daemon strategy")
        val compilerId = CompilerId.makeCompilerId(getCurrentClasspath())
        val sessionIsAliveFlagFile = buildIdToSessionFlagFile.computeIfAbsent(projectId) {
            createSessionIsAliveFlagFile()
        }

        val jvmOptions = configureDaemonJVMOptions(
            inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true
        ).also { opts ->
            val daemonJvmArguments = try {
                executionPolicy[DAEMON_JVM_ARGUMENTS]
            } catch (_: Throwable) {
                emptyList()
            }
            if (daemonJvmArguments.isNotEmpty()) {
                opts.jvmParams.addAll(
                    daemonJvmArguments.filterExtractProps(opts.mappers, "", opts.restMapper)
                )
            }
        }
        val (daemon, sessionId) = KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId, clientIsAliveFile, sessionIsAliveFlagFile, loggerAdapter, false, daemonJVMOptions = jvmOptions
        ) ?: return ExitCode.INTERNAL_ERROR.asCompilationResult
        val daemonCompileOptions = compilerArguments.toDaemonCompilationOptions(loggerAdapter.kotlinLogger)
        val isIncrementalCompilation = daemonCompileOptions is IncrementalCompilationOptions
        if (isIncrementalCompilation && daemonCompileOptions.useJvmFirRunner) {
            checkJvmFirRequirements(compilerArguments)
        }/* TODO: fix together with KT-62759
         * To avoid parsing sources from freeArgs and filtering them in the daemon,
         * work around the issue by removing .java files in non-incremental mode.
         * Preferably, this should be done in the daemon.
         * In incremental mode, incremental compiler filters them out, but should be aware of them for tracking changes.
         * Kotlin compiler itself knows about the .java sources via -Xjava-source-roots
         */
        val effectiveSources = if (isIncrementalCompilation) {
            kotlinSources
        } else {
            val kotlinFilenameExtensions =
                DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + (get(JvmCompilationOperation.Companion.KOTLINSCRIPT_EXTENSIONS) ?: emptyArray())
            kotlinSources.filter { it.toFile().isKotlinFile(kotlinFilenameExtensions) }
        }
        val arguments = compilerArguments.toCompilerArguments()
        arguments.freeArgs += effectiveSources.map { it.absolutePathString() }
        arguments.destination = destinationDirectory.absolutePathString()
        val rootProjectDir =
            (get(INCREMENTAL_COMPILATION) as? JvmSnapshotBasedIncrementalCompilationConfiguration)?.options?.get(ROOT_PROJECT_DIR)
        val exitCode = daemon.compile(
            sessionId,
            arguments.toArgumentStrings().toTypedArray(),
            daemonCompileOptions,
            BasicCompilerServicesWithResultsFacadeServer(loggerAdapter),
            DaemonCompilationResults(
                loggerAdapter.kotlinLogger, rootProjectDir?.toFile()
            )
        ).get()

        try {
            daemon.releaseCompileSession(sessionId)
        } catch (e: RemoteException) {
            loggerAdapter.kotlinLogger.warn("Unable to release compile session, maybe daemon is already down: $e")
        }

        return (ExitCode.entries.find { it.code == exitCode } ?: if (exitCode == 0) {
            ExitCode.OK
        } else {
            ExitCode.COMPILATION_ERROR
        }).asCompilationResult

    }

    private fun getCurrentClasspath() =
        (JvmCompilationOperationImpl::class.java.classLoader as URLClassLoader).urLs.map { transformUrlToFile(it) }


    private fun compileInProcess(loggerAdapter: KotlinLoggerMessageCollectorAdapter): CompilationResult {
        loggerAdapter.kotlinLogger.debug("Compiling using the in-process strategy")
        setupIdeaStandaloneExecution()
        val compiler = K2JVMCompiler()
        val arguments = compilerArguments.toCompilerArguments()
        val kotlinFilenameExtensions =
            DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS + (get(JvmCompilationOperation.Companion.KOTLINSCRIPT_EXTENSIONS) ?: emptyArray())
        arguments.destination = destinationDirectory.absolutePathString()
        val aggregatedIcConfiguration = get(INCREMENTAL_COMPILATION)
        return when (aggregatedIcConfiguration) {
            is JvmSnapshotBasedIncrementalCompilationConfiguration -> {
                @Suppress("DEPRECATION") val kotlinSources = extractKotlinSourcesFromFreeCompilerArguments(
                    arguments, DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
                ) + kotlinSources.map { it.toFile() }

                val classpathChanges = aggregatedIcConfiguration.classpathChanges
                val buildReporter = BuildReporter(
                    icReporter = BuildToolsApiBuildICReporter(
                        loggerAdapter.kotlinLogger, aggregatedIcConfiguration.options[ROOT_PROJECT_DIR].toFile()
                    ), buildMetricsReporter = DoNothingBuildMetricsReporter
                )
                val verifiedPreciseJavaTracking =
                    arguments.disablePreciseJavaTrackingIfK2(usePreciseJavaTrackingByDefault = aggregatedIcConfiguration.options[PRECISE_JAVA_TRACKING])
                val icFeatures = aggregatedIcConfiguration.extractIncrementalCompilationFeatures().copy(
                    usePreciseJavaTracking = verifiedPreciseJavaTracking
                )
                val incrementalCompiler =
                    if (aggregatedIcConfiguration.options[USE_FIR_RUNNER] && checkJvmFirRequirements(compilerArguments)) {
                        IncrementalFirJvmCompilerRunner(
                            aggregatedIcConfiguration.workingDirectory.toFile(),
                            buildReporter,
                            outputDirs = aggregatedIcConfiguration.options[OUTPUT_DIRS]?.map { it.toFile() },
                            classpathChanges = classpathChanges,
                            kotlinSourceFilesExtensions = kotlinFilenameExtensions,
                            icFeatures = icFeatures
                        )
                    } else {
                        IncrementalJvmCompilerRunner(
                            aggregatedIcConfiguration.workingDirectory.toFile(),
                            buildReporter,
                            outputDirs = aggregatedIcConfiguration.options[OUTPUT_DIRS]?.map { it.toFile() },
                            classpathChanges = classpathChanges,
                            kotlinSourceFilesExtensions = kotlinFilenameExtensions,
                            icFeatures = icFeatures
                        )
                    }

                val rootProjectDir = aggregatedIcConfiguration.options[ROOT_PROJECT_DIR]
                val buildDir = aggregatedIcConfiguration.options[MODULE_BUILD_DIR]
                arguments.incrementalCompilation = true
                incrementalCompiler.compile(
                    kotlinSources,
                    arguments,
                    loggerAdapter,
                    aggregatedIcConfiguration.sourcesChanges.asChangedFiles,
                    fileLocations = FileLocations(rootProjectDir.toFile(), buildDir.toFile())
                ).asCompilationResult
            }
            null -> { // no IC configuration -> non-incremental compilation
                arguments.freeArgs += kotlinSources.filter { it.toFile().isKotlinFile(kotlinFilenameExtensions) }
                    .map { it.absolutePathString() }
                compiler.exec(loggerAdapter, Services.EMPTY, arguments).asCompilationResult
            }
            else -> error(
                "Unexpected incremental compilation configuration: $aggregatedIcConfiguration. " + "In this version, it must be an instance of JvmSnapshotBasedIncrementalCompilationConfiguration " + "for incremental compilation, or null for non-incremental compilation."
            )
        }
    }
}

private fun JvmSnapshotBasedIncrementalCompilationConfiguration.extractIncrementalCompilationFeatures(): IncrementalCompilationFeatures =
    IncrementalCompilationFeatures(
        usePreciseJavaTracking = options[PRECISE_JAVA_TRACKING],
        withAbiSnapshot = false,
        preciseCompilationResultsBackup = options[BACKUP_CLASSES], //TODO is it this option?
        keepIncrementalCompilationCachesInMemory = options[KEEP_IC_CACHES_IN_MEMORY],
    )

private val JvmSnapshotBasedIncrementalCompilationConfiguration.classpathChanges: ClasspathChanges.ClasspathSnapshotEnabled
    get() {
        val snapshotFiles =
            ClasspathSnapshotFiles(dependenciesSnapshotFiles.map { it.toFile() }, shrunkClasspathSnapshot.toFile().parentFile)
        return when {
            !shrunkClasspathSnapshot.exists() -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot(
                snapshotFiles
            )
            options[FORCE_RECOMPILATION] -> ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun(
                snapshotFiles
            )
            options[ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES] -> ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.NoChanges(
                snapshotFiles
            )
            else -> {
                ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(snapshotFiles)
            }
        }
    }


private fun checkJvmFirRequirements(
    arguments: JvmCompilerArgumentsImpl,
): Boolean {
    val languageVersion: LanguageVersion = try {
        arguments[LANGUAGE_VERSION]
    } catch (_: Exception) {
        null
    }?.let {
        LanguageVersion.fromVersionString(it.value)
    } ?: LanguageVersion.LATEST_STABLE

    check(languageVersion >= LanguageVersion.KOTLIN_2_0) {
        "FIR incremental compiler runner is only compatible with Kotlin Language Version 2.0"
    }
    @Suppress("DEPRECATION") check(arguments[XUSE_FIR_IC]) {
        "FIR incremental compiler runner requires '-Xuse-fir-ic' to be present in arguments"
    }

    return true
}


private fun <T : CommonToolArguments> KClass<T>.newArgumentsInstance(): T {
    val argumentConstructor = constructors.find { it.parameters.isEmpty() } ?: throw IllegalArgumentException(
        "$qualifiedName has no empty constructor"
    )
    return argumentConstructor.call()
}

@PublishedApi
internal fun <T : CommonToolArguments> toArgumentStrings(
    thisArguments: T, type: KClass<T>,
    shortArgumentKeys: Boolean,
    compactArgumentValues: Boolean,
): List<String> = ArrayList<String>().apply {
    val defaultArguments = type.newArgumentsInstance()
    type.memberProperties.forEach { property ->
        val argumentAnnotation = property.javaField?.getAnnotation(Argument::class.java) ?: return@forEach
        val rawPropertyValue = property.get(thisArguments)
        val rawDefaultValue = property.get(defaultArguments)

        /* Default value can be omitted */
        if (rawPropertyValue == rawDefaultValue) {
            return@forEach
        }

        val argumentStringValues = when {
            property.returnType.classifier == Boolean::class -> listOf(rawPropertyValue?.toString() ?: false.toString())

            (property.returnType.classifier as? KClass<*>)?.java?.isArray == true -> getArgumentStringValue(
                argumentAnnotation,
                rawPropertyValue as Array<*>?,
                compactArgumentValues
            )

            property.returnType.classifier == List::class -> getArgumentStringValue(
                argumentAnnotation,
                (rawPropertyValue as List<*>?)?.toTypedArray(),
                compactArgumentValues
            )

            else -> listOf(rawPropertyValue.toString())
        }

        val argumentName = if (shortArgumentKeys && argumentAnnotation.shortName.isNotEmpty()) argumentAnnotation.shortName
        else argumentAnnotation.value

        argumentStringValues.forEach { argumentStringValue ->

            when {/* We can just enable the flag by passing the argument name like -myFlag: Value not required */
                rawPropertyValue is Boolean && rawPropertyValue -> {
                    add(argumentName)
                }

                /* Advanced (e.g. -X arguments) or boolean properties need to be passed using the '=' */
                argumentAnnotation.isAdvanced || property.returnType.classifier == Boolean::class -> {
                    add("$argumentName=$argumentStringValue")
                }
                else -> {
                    add(argumentName)
                    add(argumentStringValue)
                }
            }
        }
    }

    addAll(thisArguments.freeArgs)
    addAll(thisArguments.internalArguments.map { it.stringRepresentation })
}


private fun getArgumentStringValue(argumentAnnotation: Argument, values: Array<*>?, compactArgumentValues: Boolean): List<String> {
    if (values.isNullOrEmpty()) return emptyList()
    val delimiter = argumentAnnotation.resolvedDelimiter
    return if (delimiter.isNullOrEmpty() || !compactArgumentValues) values.map { it.toString() }
    else listOf(values.joinToString(delimiter))
}