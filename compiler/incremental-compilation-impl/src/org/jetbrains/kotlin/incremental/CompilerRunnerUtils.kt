/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CompilerRunnerUtils")

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.DoNothingICReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import java.io.File

var K2JVMCompilerArguments.destinationAsFile: File
    get() = File(destination)
    set(value) {
        destination = value.absolutePath
    }

var K2JVMCompilerArguments.classpathAsList: List<File>
    get() = classpath.orEmpty().split(File.pathSeparator).map(::File)
    set(value) {
        classpath = value.joinToString(separator = File.pathSeparator, transform = { it.absolutePath })
    }

val K2JVMCompilerArguments.isK1ForcedByKapt: Boolean
    // coordinated with org.jetbrains.kotlin.cli.jvm.K2JVMCompiler.shouldRunK2
    get() {
        val isK2 = languageVersion?.startsWith('2') != false
        val isKaptUsed = pluginOptions?.any { it.startsWith("plugin:org.jetbrains.kotlin.kapt3") } == true
        return isK2 && isKaptUsed && useK2Kapt == false
    }

fun K2JVMCompilerArguments.disablePreciseJavaTrackingIfK2(usePreciseJavaTrackingByDefault: Boolean): Boolean {
    // TODO: This should be removed after implementing of fir-based java tracker (KT-57147).
    //  See org.jetbrains.kotlin.incremental.CompilerRunnerUtilsKt.makeJvmIncrementally
    val languageVersion = if (isK1ForcedByKapt) {
        LanguageVersion.KOTLIN_1_9
    } else {
        LanguageVersion.fromVersionString(languageVersion) ?: LanguageVersion.LATEST_STABLE
    }
    return !languageVersion.usesK2 && usePreciseJavaTrackingByDefault
}

@Suppress("unused") // used in Maven compile runner
fun makeJvmIncrementally(
    cachesDir: File,
    sourceRoots: Iterable<File>,
    args: K2JVMCompilerArguments,
    messageCollector: MessageCollector = MessageCollector.NONE,
    reporter: ICReporter = DoNothingICReporter
) {
    val kotlinExtensions = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
    val allExtensions = kotlinExtensions + "java"
    val rootsWalk = sourceRoots.asSequence().flatMap { it.walk() }
    val files = rootsWalk.filter(File::isFile)
    val sourceFiles = files.filter { it.extension.lowercase() in allExtensions }.toList()
    val buildHistoryFile = File(cachesDir, "build-history.bin")
    args.javaSourceRoots = sourceRoots.map { it.absolutePath }.toTypedArray()
    val buildReporter = BuildReporter(icReporter = reporter, buildMetricsReporter = DoNothingBuildMetricsReporter)

    withIncrementalCompilation(args) {
        val verifiedPreciseJavaTracking = args.disablePreciseJavaTrackingIfK2(usePreciseJavaTrackingByDefault = true)

        val compiler = BuildHistoryJvmICRunner(
            cachesDir,
            buildReporter,
            buildHistoryFile = buildHistoryFile,
            outputDirs = null,
            modulesApiHistory = EmptyModulesApiHistory,
            kotlinSourceFilesExtensions = kotlinExtensions,
            icFeatures = IncrementalCompilationFeatures(
                usePreciseJavaTracking = verifiedPreciseJavaTracking
            ),
        )
        compiler.compile(sourceFiles, args, messageCollector, changedFiles = ChangedFiles.DeterminableFiles.ToBeComputed)
    }
}

@Suppress("DEPRECATION")
inline fun <R> withIncrementalCompilation(args: CommonCompilerArguments, enabled: Boolean = true, fn: () -> R): R {
    val isEnabledBackup = IncrementalCompilation.isEnabledForJvm()
    IncrementalCompilation.setIsEnabledForJvm(enabled)

    try {
        if (args.incrementalCompilation == null) {
            args.incrementalCompilation = enabled
        }
        return fn()
    } finally {
        IncrementalCompilation.setIsEnabledForJvm(isEnabledBackup)
    }
}
