/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JsProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.assumeJsIsSupported

class JsScenarioDsl(
    override val project: JsProject,
    override val strategyConfig: ExecutionPolicy,
    override val kotlinToolchains: KotlinToolchains,
) : Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>

fun BaseCompilationTest.jsScenario(
    kotlinToolchains: KotlinToolchains,
    strategyConfig: ExecutionPolicy,
    action: Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    kotlinToolchains.assumeJsIsSupported()
    action(JsScenarioDsl(JsProject(kotlinToolchains, strategyConfig, workingDirectory), strategyConfig, kotlinToolchains))
}

fun BaseCompilationTest.jsScenario(
    executionStrategy: CompilerExecutionStrategyConfiguration,
    action: Scenario<JsKlibCompilationOperation.Builder, JsHistoryBasedIncrementalCompilationConfiguration.Builder>.() -> Unit,
) {
    jsScenario(executionStrategy.first, executionStrategy.second, action)
}
