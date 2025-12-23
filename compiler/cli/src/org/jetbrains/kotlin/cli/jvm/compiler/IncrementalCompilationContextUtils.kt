/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.ProjectFileSearchScopeProvider
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.modules.TargetId

fun createIncrementalCompilationScope(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    incrementalExcludesScope: AbstractProjectFileSearchScope?
): AbstractProjectFileSearchScope? {
    if (configuration.get(JVMConfigurationKeys.MODULES) == null) {
        return null
    }

    val incrementalCompilationComponents = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
    if (incrementalCompilationComponents == null) {
        return null
    } else if (incrementalCompilationComponents is ProjectFileSearchScopeProvider) {
        return incrementalCompilationComponents.createSearchScope(projectEnvironment)
    }

    val dir = configuration[JVMConfigurationKeys.OUTPUT_DIRECTORY] ?: return null
    return projectEnvironment.getSearchScopeByDirectories(setOf(dir)).let {
        if (incrementalExcludesScope?.isEmpty != false) it
        else it - incrementalExcludesScope
    }
}

fun createContextForIncrementalCompilation(
    configuration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    sourceScope: AbstractProjectFileSearchScope,
    previousStepsSymbolProviders: List<FirSymbolProvider>,
    incrementalCompilationScope: AbstractProjectFileSearchScope?
): IncrementalCompilationContext? {
    if (incrementalCompilationScope == null && previousStepsSymbolProviders.isEmpty()) return null
    val targetIds = configuration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId) ?: return null
    val incrementalComponents = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS) ?: return null

    return IncrementalCompilationContext(
        previousStepsSymbolProviders,
        IncrementalPackagePartProvider(
            projectEnvironment.getPackagePartProvider(sourceScope),
            targetIds.map(incrementalComponents::getIncrementalCache)
        ),
        incrementalCompilationScope
    )
}

fun createContextForIncrementalCompilation(
    projectEnvironment: VfsBasedProjectEnvironment,
    moduleConfiguration: CompilerConfiguration,
    sourceScope: AbstractProjectFileSearchScope,
): IncrementalCompilationContext? {
    val incrementalComponents = moduleConfiguration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
    val targetIds = moduleConfiguration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)

    if (targetIds == null || incrementalComponents == null) return null
    val directoryWithIncrementalPartsFromPreviousCompilation =
        moduleConfiguration[JVMConfigurationKeys.OUTPUT_DIRECTORY]
            ?: return null
    val incrementalCompilationScope = directoryWithIncrementalPartsFromPreviousCompilation.walk()
        .filter { it.extension == "class" }
        .let { projectEnvironment.getSearchScopeByIoFiles(it.asIterable()) }
        .takeIf { !it.isEmpty }
        ?: return null
    val packagePartProvider = IncrementalPackagePartProvider(
        projectEnvironment.getPackagePartProvider(sourceScope),
        targetIds.map(incrementalComponents::getIncrementalCache)
    )
    return IncrementalCompilationContext(emptyList(), packagePartProvider, incrementalCompilationScope)
}
