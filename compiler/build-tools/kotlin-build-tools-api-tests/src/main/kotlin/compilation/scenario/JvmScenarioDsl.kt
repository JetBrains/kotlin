/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JvmProject

private class JvmScenarioDsl(
    override val project: JvmProject,
    override val strategyConfig: ExecutionPolicy,
    override val kotlinToolchains: KotlinToolchains,
) : Scenario<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>

fun BaseCompilationTest.jvmScenario(
    kotlinToolchains: KotlinToolchains,
    strategyConfig: ExecutionPolicy,
    action: Scenario<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    action(JvmScenarioDsl(JvmProject(kotlinToolchains, strategyConfig, workingDirectory), strategyConfig, kotlinToolchains))
}

fun BaseCompilationTest.jvmScenario(
    executionStrategy: CompilerExecutionStrategyConfiguration,
    action: Scenario<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    jvmScenario(executionStrategy.first, executionStrategy.second, action)
}

