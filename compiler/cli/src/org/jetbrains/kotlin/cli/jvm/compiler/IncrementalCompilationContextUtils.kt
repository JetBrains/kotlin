/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.utils.addToStdlib.takeIfNotEmpty

/**
 * Used by
 * - JVM compiler
 * - metadata LT compiler
 * - scripting plugin
 *
 * Should be used in par with [createContextForIncrementalCompilation]
 */
fun createIncrementalCompilationScope(
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

/**
 * Used by
 * - JVM compiler
 * - metadata LT compiler
 * - scripting plugin
 *
 * Should be used in par with [createIncrementalCompilationScope]
 */
fun createContextForIncrementalCompilation(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    sourceScope: AbstractProjectFileSearchScope,
    previousStepsSymbolProviders: List<FirSymbolProvider>,
    incrementalCompilationScope: AbstractProjectFileSearchScope?
): IncrementalCompilationContext? {
    if (incrementalCompilationScope == null && previousStepsSymbolProviders.isEmpty()) return null
    val targetIds = configuration.modules.map(::TargetId).takeIfNotEmpty() ?: return null
    val incrementalComponents = configuration.incrementalCompilationComponents ?: return null

    return IncrementalCompilationContext(
        previousFirSessionsSymbolProviders = previousStepsSymbolProviders,
        precompiledBinariesPackagePartProvider = IncrementalPackagePartProvider(
            projectEnvironment.getPackagePartProvider(sourceScope),
            targetIds.map(incrementalComponents::getIncrementalCache)
        ),
        precompiledBinariesFileScope = incrementalCompilationScope
    )
}

/**
 * Used by metadata PSI compiler
 */
fun createContextForIncrementalCompilation(
    projectEnvironment: VfsBasedProjectEnvironment,
    moduleConfiguration: CompilerConfiguration,
    sourceScope: AbstractProjectFileSearchScope,
): IncrementalCompilationContext? {
    val incrementalComponents = moduleConfiguration.incrementalCompilationComponents ?: return null
    val targetIds = moduleConfiguration.modules.map(::TargetId).takeIfNotEmpty() ?: return null

    val directoryWithIncrementalPartsFromPreviousCompilation = moduleConfiguration.outputDirectory ?: return null
    val incrementalCompilationScope = directoryWithIncrementalPartsFromPreviousCompilation.walk()
        .filter { it.extension == "class" }
        .let { projectEnvironment.getSearchScopeByIoFiles(it.asIterable()) }
        .takeIf { !it.isEmpty }
        ?: return null
    val packagePartProvider = IncrementalPackagePartProvider(
        projectEnvironment.getPackagePartProvider(sourceScope),
        targetIds.map(incrementalComponents::getIncrementalCache)
    )
    return IncrementalCompilationContext(
        previousFirSessionsSymbolProviders = emptyList(),
        precompiledBinariesPackagePartProvider = packagePartProvider,
        precompiledBinariesFileScope = incrementalCompilationScope
    )
}
