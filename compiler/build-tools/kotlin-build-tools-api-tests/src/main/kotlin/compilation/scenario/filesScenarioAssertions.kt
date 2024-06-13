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
context(Module, ScenarioModule)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun CompilationOutcome.assertAddedOutputs(vararg addedOutputs: String) {
    assertAddedOutputs(addedOutputs.toSet())
}

/**
 * This assertion has side effects modifying the expected total outputs list!
 * If you decided to start using it, don't mix it with regular [assertOutputs]
 */
context(Module, ScenarioModule)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun CompilationOutcome.assertAddedOutputs(addedOutputs: Set<String>) {
    val outputs = requireScenarioModuleImpl().outputs
    outputs.addAll(addedOutputs)
    assertOutputs(outputs)
}

/**
 * This assertion has side effects modifying the expected total outputs list!
 * If you decided to start using it, don't mix it with regular [assertOutputs]
 */
context(Module, ScenarioModule)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun CompilationOutcome.assertRemovedOutputs(vararg removedOutputs: String) {
    assertRemovedOutputs(removedOutputs.toSet())
}

/**
 * This assertion has side effects modifying the expected total outputs list!
 * If you decided to start using it, don't mix it with regular [assertOutputs]
 */
context(Module, ScenarioModule)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun CompilationOutcome.assertRemovedOutputs(removedOutputs: Set<String>) {
    val outputs = requireScenarioModuleImpl().outputs
    val notPresentOutputs = removedOutputs - outputs
    assert(notPresentOutputs.isEmpty()) {
        "The following files were expected to be removed, however they weren't even produced: $notPresentOutputs"
    }
    outputs.removeAll(removedOutputs)
    assertOutputs(outputs)
}

context(Module, ScenarioModule)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
fun CompilationOutcome.assertNoOutputSetChanges() {
    val outputs = requireScenarioModuleImpl().outputs
    assertOutputs(outputs)
}

context(ScenarioModule)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
private fun requireScenarioModuleImpl() =
    (this@ScenarioModule as? ScenarioModuleImpl ?: error("Expected an instance of ScenarioModuleImpl"))