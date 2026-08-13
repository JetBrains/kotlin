/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.cli.jvm.config.precompiledOutputRoots
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.modules.TargetId

/**
 * The output of the previous build, which incremental compilation reads as a separate classpath.
 *
 * By default that is this build's own output directory: a compilation driven by command line arguments gets
 * the previous output prepended to its classpath, see `IncrementalJvmCompilerRunnerBase.performWorkBeforeCompilation`.
 * A build system which registers its content roots itself marks them instead ([precompiledOutputRoots]) — the
 * IntelliJ build system does, because its output is not a directory on disk at all.
 */
private fun CompilerConfiguration.precompiledBinariesClasspath(): JvmClasspath.Roots? {
    if (modules.isEmpty()) return null
    if (incrementalCompilationComponents == null) return null

    val roots = precompiledOutputRoots().takeIf { it.isNotEmpty() }
        ?: listOf(JvmClasspathRootId.of((outputDirectory ?: return null).toPath()))
    return JvmClasspath.Roots(roots)
}

fun prepareIncrementalCompilationContextAndLibrariesClasspath(
    configuration: CompilerConfiguration,
): Pair<JvmClasspath, IncrementalCompilationContext?> {
    val precompiledBinaries = configuration.precompiledBinariesClasspath()
        ?: return JvmClasspath.ProjectLibraries() to null

    val targetIds = configuration.modules.map(::TargetId)
    val incrementalComponents = configuration.incrementalCompilationComponents!!

    val context = IncrementalCompilationContext(
        precompiledBinariesPackagePartProvider = IncrementalPackagePartProvider(
            configuration.languageVersionSettings,
            targetIds.map(incrementalComponents::getIncrementalCache)
        ),
        precompiledBinaries = precompiledBinaries
    )
    /*
     * The precompiled binaries are read twice — once as the regular classpath and once as the precompiled
     * binaries of this build — so they have to be taken out of the first one, otherwise one big
     * `JvmPackagePartProvider` would serve both symbol providers.
     *
     * See also the corresponding comment in `IncrementalJvmCompilerRunnerBase.performWorkBeforeCompilation`
     */
    return JvmClasspath.ProjectLibraries(excludedRoots = precompiledBinaries.roots) to context
}
