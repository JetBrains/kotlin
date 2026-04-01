/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.cli.jvm.compiler.createLibraryListForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.legacy.pipeline.FrontendContext
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.name.Name

/**
 * Creates library session and sources session for JVM platform
 * Number of created session depends on mode of MPP:
 *   - disabled
 *   - legacy (one platform and one common module)
 *   - HMPP (multiple number of modules)
 */
@LegacyK2CliPipeline
fun <F> FrontendContext.prepareJvmSessions(
    files: List<F>,
    rootModuleNameAsString: String,
    friendPaths: List<String>,
    librariesScope: AbstractProjectFileSearchScope,
    isCommonSource: (F) -> Boolean,
    isScript: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    createProviderAndScopeForIncrementalCompilation: (List<F>) -> IncrementalCompilationContext?,
): List<SessionWithSources<F>> {
    val libraryList = createLibraryListForJvm(rootModuleNameAsString, configuration, friendPaths)
    val rootModuleName = Name.special("<$rootModuleNameAsString>")
    return prepareJvmSessions(
        files, rootModuleName, librariesScope, libraryList,
        isCommonSource, isScript, fileBelongsToModule, createProviderAndScopeForIncrementalCompilation
    )
}

@LegacyK2CliPipeline
fun <F> FrontendContext.prepareJvmSessions(
    files: List<F>,
    rootModuleName: Name,
    librariesScope: AbstractProjectFileSearchScope,
    libraryList: DependencyListForCliModule,
    isCommonSource: (F) -> Boolean,
    isScript: (F) -> Boolean,
    fileBelongsToModule: (F, String) -> Boolean,
    createProviderAndScopeForIncrementalCompilation: (List<F>) -> IncrementalCompilationContext?,
): List<SessionWithSources<F>> {
    return JvmFrontendPipelinePhase.prepareJvmSessions(
        files,
        rootModuleName,
        configuration,
        projectEnvironment,
        librariesScope,
        libraryList,
        isCommonSource,
        isScript,
        fileBelongsToModule,
        createProviderAndScopeForIncrementalCompilation,
    )
}
