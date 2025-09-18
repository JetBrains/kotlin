/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.internal.ClasspathEntrySnapshotImpl
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.copyOf
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import java.io.File
import java.security.MessageDigest

fun calculateCompilationInputHash(
    args: K2JVMCompilerArguments
): String {
    // TODO: at this stage I'm computing input hash with these arguments
    // this needs to be revisited as they might be other important arguments that can produce
    // different compilation results

    // TODO: consider creating approach where we create new object with important fields
    // it will be less error prone as this modifying of original compiler arguments
    val importantCompilerArgs = args.copyOf()
    importantCompilerArgs.destination = null
    importantCompilerArgs.classpath = null
    importantCompilerArgs.pluginClasspaths = null
    importantCompilerArgs.freeArgs = importantCompilerArgs.freeArgs.filter { !it.startsWith("/") }

    val digest = MessageDigest.getInstance("SHA-256")
    val files = CompilerUtils.getSourceFiles(args) + CompilerUtils.getDependencyFiles(args) + CompilerUtils.getXPluginFiles(args)
    files.sortedBy { it.path }.forEach { file ->
        digest.update(computeSha256(file).toByteArray(Charsets.UTF_8)) // TODO: double check this approach
    }
    ArgumentUtils.convertArgumentsToStringList(importantCompilerArgs).sorted().forEach { arg ->
        digest.update(arg.toByteArray())
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

// function taken from CompilationServiceImpl.kt, I could not access it because of access modifiers
@OptIn(ExperimentalBuildToolsApi::class)
fun calculateClasspathSnapshot(
    classpathEntry: File,
    granularity: ClassSnapshotGranularity,
    parseInlinedLocalClasses: Boolean
): ClasspathEntrySnapshot {
    return ClasspathEntrySnapshotImpl(
        ClasspathEntrySnapshotter.snapshot(
            classpathEntry,
            ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses),
            DoNothingBuildMetricsReporter
        )
    )
}