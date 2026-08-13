/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.modules.TargetId

/**
 * The output of the previous build, which incremental compilation reads as a separate classpath.
 *
 * By default that is this build's own output directory. Components implementing
 * [IncrementalCompilationComponentsWithCustomPrecompiledBinaries] name the roots themselves instead — the
 * IntelliJ build system does, because its output is not `outputDirectory`.
 */
private fun CompilerConfiguration.precompiledBinariesClasspath(): JvmClasspath.Roots? {
    if (modules.isEmpty()) return null

    val roots = when (val components = incrementalCompilationComponents ?: return null) {
        is IncrementalCompilationComponentsWithCustomPrecompiledBinaries -> components.precompiledBinariesRoots()
        else -> listOf((outputDirectory ?: return null).toPath())
    }
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
