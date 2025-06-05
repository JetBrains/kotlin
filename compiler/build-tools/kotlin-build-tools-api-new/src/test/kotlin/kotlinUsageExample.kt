/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.kotlin.buildtools.api.BaseCompilerArguments
import org.jetbrains.kotlin.buildtools.api.BaseToolArguments
import org.jetbrains.kotlin.buildtools.api.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.JvmClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.JvmClasspathSnapshottingOperation
import org.jetbrains.kotlin.buildtools.api.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain
import org.jetbrains.kotlin.buildtools.api.SourceToOutputsTracker
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.JvmTarget
import org.jetbrains.kotlin.buildtools.api.arguments.KotlinVersion
import java.nio.file.Path
import java.nio.file.Paths

suspend fun basicJvmUse(classLoader: ClassLoader) = coroutineScope {
    val kotlinToolchain = KotlinToolchain.loadImplementation(classLoader)
    val kotlinSources = listOf(Paths.get("src/a.kt"))
    val destination = Paths.get("build/classes/kotlin")
    val compilation = kotlinToolchain.jvm.createJvmCompilationOperation(kotlinSources, destination)
    compilation.compilerArguments[BaseToolArguments.VERBOSE] = true
    compilation.compilerArguments[BaseCompilerArguments.API_VERSION] = KotlinVersion.KOTLIN_2_1
    compilation.compilerArguments[BaseCompilerArguments.LANGUAGE_VERSION] = KotlinVersion.KOTLIN_2_2
    compilation.compilerArguments[BaseCompilerArguments.PROGRESSIVE] = true
    compilation.compilerArguments[BaseCompilerArguments.OPT_IN] = listOf("my.custom.OptInAnnotation")

    val allOpenPlugin = CompilerPlugin(Paths.get("../plugin-jar"), "org.jetbrains.kotlin.allopen", mapOf("preset" to "spring"))

    compilation.compilerArguments[BaseCompilerArguments.COMPILER_PLUGINS] = listOf(allOpenPlugin)

    compilation.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_21

    // a way to pass raw arbitrary arguments propagated as freeArgs
    compilation.compilerArguments[BaseToolArguments.ToolArgument.Custom("BACKEND_THREADS")] = "-Xbackend-threads=4"

    val executionPolicy = kotlinToolchain.createExecutionPolicy()
    executionPolicy[ExecutionPolicy.EXECUTION_MODE] = ExecutionPolicy.ExecutionMode.DAEMON
    executionPolicy[ExecutionPolicy.DAEMON_JVM_ARGUMENTS] = listOf("-Xmx1G")

    kotlinToolchain.executeOperation(compilation, executionPolicy)
}

suspend fun jvmIc(classLoader: ClassLoader) = coroutineScope {
    val kotlinToolchain = KotlinToolchain.loadImplementation(classLoader)

    val dependencies = listOf(
        Paths.get("lib/a.jar"),
        Paths.get("lib/b.jar"),
    )

    val snapshotOperations = dependencies
        .associateWith { origin -> origin.resolveSibling("${origin.fileName}.snapshot") }
        .map { (origin, snapshotPath) ->
            snapshotPath to kotlinToolchain.jvm.createClasspathSnapshottingOperation(origin).apply {
                set(JvmClasspathSnapshottingOperation.GRANULARITY, JvmClassSnapshotGranularity.CLASS_LEVEL)
            }
        }
//        .map { (snapshotPath, snapshot) -> snapshotPath.also { snapshot.saveSnapshot(it) } }

    val snapshots = snapshotOperations.map { (snapshotPath, operation) ->
        async {
            val snapshot = kotlinToolchain.executeOperation(operation)
            snapshot.saveSnapshot(snapshotPath)
            snapshotPath
        }
    }.awaitAll()

    val kotlinSources = listOf(Paths.get("src/a.kt"))
    val destination = Paths.get("build/classes/kotlin")
    val compilation = kotlinToolchain.jvm.createJvmCompilationOperation(kotlinSources, destination)

    val icOptions = compilation.makeSnapshotBasedIcOptions()

    icOptions[JvmSnapshotBasedIncrementalCompilationOptions.BACKUP_CLASSES] = true

    compilation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = JvmSnapshotBasedIncrementalCompilationConfiguration(
        workingDirectory = Paths.get("build/kotlin"),
        sourcesChanges = SourcesChanges.ToBeCalculated,
        dependenciesSnapshotFiles = snapshots,
        options = icOptions,
    )

    kotlinToolchain.executeOperation(compilation, kotlinToolchain.createExecutionPolicy())
}

suspend fun lookupTracker(classLoader: ClassLoader) = coroutineScope {
    val kotlinToolchain = KotlinToolchain.loadImplementation(classLoader)
    val kotlinSources = listOf(Paths.get("src/a.kt"))
    val destination = Paths.get("build/classes/kotlin")
    val compilation = kotlinToolchain.jvm.createJvmCompilationOperation(kotlinSources, destination)
    compilation[JvmCompilationOperation.LOOKUP_TRACKER] = object : CompilerLookupTracker {
        override fun recordLookup(
            filePath: String,
            position: CompilerLookupTracker.Position,
            scopeFqName: String,
            scopeKind: CompilerLookupTracker.ScopeKind,
            name: String,
        ) {
            TODO("Not yet implemented")
        }
    }
    compilation[JvmCompilationOperation.SOURCE_TO_OUTPUTS_TRACKER] = object : SourceToOutputsTracker {
        override fun recordMapping(filePath: Path, outputs: List<Path>) {
            TODO("Not yet implemented")
        }
    }
    kotlinToolchain.executeOperation(compilation)
}

suspend fun metrics(classLoader: ClassLoader) = coroutineScope {
    val kotlinToolchain = KotlinToolchain.loadImplementation(classLoader)
    val kotlinSources = listOf(Paths.get("src/a.kt"))
    val destination = Paths.get("build/classes/kotlin")
    val compilationOperation = kotlinToolchain.native.createKlibCompilationOperation(kotlinSources, destination)
    compilationOperation[BuildOperation.METRICS_COLLECTOR] = object : BuildMetricsCollector {
        override fun collectMetric(
            name: String,
            type: BuildMetricsCollector.ValueType,
            value: Long,
        ) {
            println("$name: $value ($type)")
        }
    }
    kotlinToolchain.executeOperation(compilationOperation, kotlinToolchain.createExecutionPolicy())
}