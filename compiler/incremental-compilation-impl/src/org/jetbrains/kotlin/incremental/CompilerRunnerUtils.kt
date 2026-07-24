/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CompilerRunnerUtils")

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.LanguageVersion
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

fun K2JVMCompilerArguments.disablePreciseJavaTrackingIfK2(usePreciseJavaTrackingByDefault: Boolean): Boolean {
    // TODO: This should be removed after implementing of fir-based java tracker (KT-57147).
    //  See org.jetbrains.kotlin.incremental.CompilerRunnerUtilsKt.makeJvmIncrementally
    val languageVersion = LanguageVersion.fromVersionString(languageVersion) ?: LanguageVersion.LATEST_STABLE
    return !languageVersion.usesK2 && usePreciseJavaTrackingByDefault
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
