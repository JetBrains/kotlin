/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Module

/**
 * This assertion has side effects modifying the expected total outputs list!
 * If you decided to start using it, don't mix it with regular [assertOutputs]
 */
fun CompilationOutcome.assertAddedOutputs(module: Module, scenarioModule: ScenarioModule, vararg addedOutputs: String) {
    assertAddedOutputs(module, scenarioModule, addedOutputs.toSet())
}

/**
 * This assertion has side effects modifying the expected total outputs list!
 * If you decided to start using it, don't mix it with regular [assertOutputs]
 */
fun CompilationOutcome.assertAddedOutputs(module: Module, scenarioModule: ScenarioModule, addedOutputs: Set<String>) {
    val outputs = requireScenarioModuleImpl(scenarioModule).outputs
    outputs.addAll(addedOutputs)
    assertOutputs(module, outputs)
}

/**
 * This assertion has side effects modifying the expected total outputs list!
 * If you decided to start using it, don't mix it with regular [assertOutputs]
 */
fun CompilationOutcome.assertRemovedOutputs(module: Module, scenarioModule: ScenarioModule, vararg removedOutputs: String) {
    assertRemovedOutputs(module, scenarioModule, removedOutputs.toSet())
}

/**
 * This assertion has side effects modifying the expected total outputs list!
 * If you decided to start using it, don't mix it with regular [assertOutputs]
 */
fun CompilationOutcome.assertRemovedOutputs(module: Module, scenarioModule: ScenarioModule, removedOutputs: Set<String>) {
    val outputs = requireScenarioModuleImpl(scenarioModule).outputs
    val notPresentOutputs = removedOutputs - outputs
    assert(notPresentOutputs.isEmpty()) {
        "The following files were expected to be removed, however they weren't even produced: $notPresentOutputs"
    }
    outputs.removeAll(removedOutputs)
    assertOutputs(module, outputs)
}

fun CompilationOutcome.assertNoOutputSetChanges(module: Module, scenarioModule: ScenarioModule) {
    val outputs = requireScenarioModuleImpl(scenarioModule).outputs
    assertOutputs(module, outputs)
}

private fun requireScenarioModuleImpl(scenarioModule: ScenarioModule) =
    (scenarioModule as? ScenarioModuleImpl ?: error("Expected an instance of ScenarioModuleImpl"))