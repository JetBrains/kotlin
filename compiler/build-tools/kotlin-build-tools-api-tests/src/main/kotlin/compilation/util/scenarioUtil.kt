/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.util

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.Scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.ScenarioModule

@OptIn(ExperimentalCompilerArgument::class)
fun Scenario.moduleWithFir(
    moduleName: String,
    dependencies: List<ScenarioModule> = emptyList(),
    compilationOperationConfig: (JvmCompilationOperation.Builder) -> Unit = {},
) = module(
    moduleName = moduleName,
    dependencies = dependencies,
    compilationConfigAction = {
        it.compilerArguments[CommonCompilerArguments.X_USE_FIR_IC] = true
        compilationOperationConfig(it)
    },
    icOptionsConfigAction = {
        it[USE_FIR_RUNNER] = true
    }
)
