/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.BaseCompilerArguments
import org.jetbrains.kotlin.buildtools.api.BaseToolArguments
import org.jetbrains.kotlin.buildtools.api.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.JvmClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.JvmDumbIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.JvmDumbIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.JvmIcLookupTracker
import org.jetbrains.kotlin.buildtools.api.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.JvmTarget
import org.jetbrains.kotlin.buildtools.api.arguments.KotlinVersion
import java.nio.file.Paths

fun basicJvmUse(classLoader: ClassLoader) {
    val kotlinToolchain = KotlinToolchain.loadImplementation(classLoader)
    val compilation = kotlinToolchain.jvm.makeJvmCompilationOperation()
    compilation.compilerArguments[BaseToolArguments.VERBOSE] = true
    compilation.compilerArguments[BaseCompilerArguments.API_VERSION] = KotlinVersion.KOTLIN_2_1
    compilation.compilerArguments[BaseCompilerArguments.LANGUAGE_VERSION] = KotlinVersion.KOTLIN_2_2
    compilation.compilerArguments[BaseCompilerArguments.PROGRESSIVE] = true
    compilation.compilerArguments[BaseCompilerArguments.OPT_IN] = listOf("my.custom.OptInAnnotation")

    val allOpenPlugin = CompilerPlugin(Paths.get("../plugin-jar"), "org.jetbrains.kotlin.allopen", mapOf("preset" to "spring"))

    compilation.compilerArguments[BaseCompilerArguments.COMPILER_PLUGINS] = listOf(allOpenPlugin)

    compilation.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_21
    compilation.compilerArguments[JvmCompilerArguments.DESTINATION] = Paths.get("build/classes/kotlin")

    // a way to pass raw arbitrary arguments propagated as freeArgs
    compilation.compilerArguments[BaseToolArguments.ToolArgument.Custom("BACKEND_THREADS")] = "-Xbackend-threads=4"

    val executionPolicy = kotlinToolchain.makeExecutionPolicy()
    executionPolicy[ExecutionPolicy.EXECUTION_MODE] = ExecutionPolicy.ExecutionMode.DAEMON
    executionPolicy[ExecutionPolicy.DAEMON_JVM_ARGUMENTS] = listOf("-Xmx1G")

    kotlinToolchain.executeOperation(compilation, executionPolicy)
}

fun jvmIc(classLoader: ClassLoader) {
    val kotlinToolchain = KotlinToolchain.loadImplementation(classLoader)

    val dependencies = listOf(
        Paths.get("lib/a.jar"),
        Paths.get("lib/b.jar"),
    )

    val snapshots = dependencies
        .associate { origin -> origin to origin.resolveSibling("${origin.fileName}.snapshot") }
        .map { (origin, snapshotPath) -> snapshotPath to kotlinToolchain.jvm.calculateClasspathSnapshot(origin, granularity = JvmClassSnapshotGranularity.CLASS_LEVEL) }
        .map { (snapshotPath, snapshot) -> snapshotPath.also { snapshot.saveSnapshot(it) } }

    val compilation = kotlinToolchain.jvm.makeJvmCompilationOperation()

    val icOptions = compilation.makeSnapshotBasedIcOptions()

    icOptions[JvmSnapshotBasedIncrementalCompilationOptions.BACKUP_CLASSES] = true

    compilation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = JvmSnapshotBasedIncrementalCompilationConfiguration(
        workingDirectory = Paths.get("build/kotlin"),
        sourcesChanges = SourcesChanges.ToBeCalculated,
        dependenciesSnapshotFiles = snapshots,
        options = icOptions,
    )

    kotlinToolchain.executeOperation(compilation, kotlinToolchain.makeExecutionPolicy())
}

fun lookupTracker(classLoader: ClassLoader) {
    val kotlinToolchain = KotlinToolchain.loadImplementation(classLoader)
    val compilation = kotlinToolchain.jvm.makeJvmCompilationOperation()
    val dumbIcOptions = compilation.makeDumbIcOptions()
    dumbIcOptions[JvmDumbIncrementalCompilationOptions.LOOKUP_TRACKER] = object : JvmIcLookupTracker {
        override fun recordLookup(
            filePath: String,
            position: JvmIcLookupTracker.Position,
            scopeFqName: String,
            scopeKind: JvmIcLookupTracker.ScopeKind,
            name: String,
        ) {
            TODO("Not yet implemented")
        }
    }
    compilation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = JvmDumbIncrementalCompilationConfiguration(dumbIcOptions)
    kotlinToolchain.executeOperation(compilation)
}

fun metrics(classLoader: ClassLoader) {
    val kotlinToolchain = KotlinToolchain.loadImplementation(classLoader)
    val compilationOperation = kotlinToolchain.native.makeKlibCompilationOperation()
    compilationOperation[BuildOperation.METRICS_COLLECTOR] = object : BuildMetricsCollector {
        override fun collectMetric(
            name: String,
            type: BuildMetricsCollector.ValueType,
            value: Long,
        ) {
            println("$name: $value ($type)")
        }
    }
    kotlinToolchain.executeOperation(compilationOperation, kotlinToolchain.makeExecutionPolicy())
}