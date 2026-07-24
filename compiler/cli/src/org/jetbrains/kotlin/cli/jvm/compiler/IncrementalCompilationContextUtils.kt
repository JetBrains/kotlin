/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.modules.TargetId

private fun createIncrementalCompilationScope(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    incrementalExcludesScope: AbstractProjectFileSearchScope?
): AbstractProjectFileSearchScope? {
    if (configuration.modules.isEmpty()) return null
    if (configuration.incrementalCompilationComponents == null) return null

    val dir = configuration.outputDirectory ?: return null
    return projectEnvironment.getSearchScopeByDirectories(setOf(dir)).let {
        if (incrementalExcludesScope?.isEmpty != false) it
        else it - incrementalExcludesScope
    }
}

fun prepareIncrementalCompilationContextAndLibrariesScope(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    incrementalExcludesScope: AbstractProjectFileSearchScope?
): Pair<AbstractProjectFileSearchScope, IncrementalCompilationContext?> {
    val incrementalCompilationScope = createIncrementalCompilationScope(configuration, projectEnvironment, incrementalExcludesScope)

    val originalLibrariesScope = projectEnvironment.getSearchScopeForProjectLibraries()
    if (incrementalCompilationScope == null) return originalLibrariesScope to null
    val targetIds = configuration.modules.map(::TargetId)
    val incrementalComponents = configuration.incrementalCompilationComponents!!

    val context = IncrementalCompilationContext(
        precompiledBinariesPackagePartProvider = IncrementalPackagePartProvider(
            configuration.languageVersionSettings,
            targetIds.map(incrementalComponents::getIncrementalCache)
        ),
        precompiledBinariesFileScope = incrementalCompilationScope
    )
    /*
     * This is required because JVM dependencies are handled using the IJ infrastructure in the compiler, which creates
     * one big index over all possible binaries and then allows to restrict it for callers using search scopes.
     *
     * So in IC one big `JvmPackagePartProvider` is created for both regular classpath and incremental classpath,
     * which is then split into two symbol providers.
     *
     * When we stop using IJ for JVM dependencies traversal, we can remove this hack (OSIP-191).
     *
     * See also the corresponding comment in `IncrementalJvmCompilerRunnerBase.performWorkBeforeCompilation`
     */
    val librariesScope = originalLibrariesScope - incrementalCompilationScope
    return librariesScope to context
}
