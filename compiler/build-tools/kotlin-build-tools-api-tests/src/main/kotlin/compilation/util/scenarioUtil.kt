/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.util

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.Companion.INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.SnapshotConfig
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.Scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.ScenarioModule

fun Scenario.moduleWithoutInlineSnapshotting(
    moduleName: String,
    dependencies: List<ScenarioModule>,
) = module(
    moduleName = moduleName,
    dependencies = dependencies,
    snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, false),
)

@OptIn(ExperimentalCompilerArgument::class)
fun Scenario.moduleWithFir(
    moduleName: String,
    compilationOperationConfig: (JvmCompilationOperation) -> Unit = {},
) = module(
    moduleName = moduleName,
    compilationOperationConfig = {
        compilationOperationConfig(it)
        it.compilerArguments[CommonCompilerArguments.Companion.X_USE_FIR_IC] = true
        (it[INCREMENTAL_COMPILATION] as? JvmSnapshotBasedIncrementalCompilationConfiguration)?.options[USE_FIR_RUNNER] = true
    }
)
