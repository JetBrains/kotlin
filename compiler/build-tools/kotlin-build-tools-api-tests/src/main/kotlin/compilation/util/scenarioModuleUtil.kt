/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.util

import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertExactOutput
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.ScenarioModule

/**
 * Shorthands for commonly used scenario steps & assertions
 */

fun ScenarioModule.compile(expectedDirtySet: Set<String>) {
    compile { module, scenarioModule ->
        assertCompiledSources(module, expectedDirtySet)
    }
}

fun ScenarioModule.execute(mainClass: String, exactOutput: String) {
    executeCompiledCode(mainClass) {
        assertExactOutput(exactOutput)
    }
}
