/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.pipeline

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import java.io.File

// ---

data class ModuleCompilerInput(
    val targetId: TargetId,
    val commonPlatform: TargetPlatform,
    val commonSources: Collection<File>,
    val platform: TargetPlatform,
    val platformSources: Collection<File>,
    val configuration: CompilerConfiguration
)

data class ModuleCompilerEnvironment(
    val projectEnvironment: AbstractProjectEnvironment,
    val diagnosticsReporter: BaseDiagnosticsCollector
)

data class ModuleCompilerOutput(
    val mainClassName: FqName?,
    val generationState: GenerationState
)

// ---

