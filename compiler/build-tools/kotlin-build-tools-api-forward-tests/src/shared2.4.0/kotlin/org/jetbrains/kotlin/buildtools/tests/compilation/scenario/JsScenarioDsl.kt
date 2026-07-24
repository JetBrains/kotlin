/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.forward.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.JsProject
import org.junit.jupiter.api.Assumptions

class JsScenarioDsl(
    override val project: JsProject,
    override val strategyConfig: ExecutionPolicy,
    override val kotlinToolchains: KotlinToolchains,
) : Scenario<BaseCompilationOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>

fun BaseCompilationTest.jsScenario(
    kotlinToolchains: KotlinToolchains,
    strategyConfig: ExecutionPolicy,
    action: Scenario<BaseCompilationOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    Assumptions.abort<Unit> { "Not supported in this API version" }
}

fun BaseCompilationTest.jsScenario(
    executionStrategy: CompilerExecutionStrategyConfiguration,
    action: Scenario<BaseCompilationOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    jsScenario(executionStrategy.first, executionStrategy.second, action)
}
