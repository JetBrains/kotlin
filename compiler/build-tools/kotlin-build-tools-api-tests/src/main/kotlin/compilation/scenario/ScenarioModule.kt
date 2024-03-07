/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.Module

interface ScenarioModule {
    /**
     * Performs registered existing file modification.
     *
     * Prefer the overload with versioned file modification if it's possible
     */
    fun changeFile(
        fileName: String,
        transform: (String) -> String,
    )

    /**
     * Performs registered existing file modification by copying a revision from
     * the file located by path "[fileName].[version]" in the module sources directory.
     *
     * Check the example `ExampleIncrementalScenarioTest.testScenario4` out
     */
    fun changeFile(
        fileName: String,
        version: UInt,
    )

    /**
     * Performs registered file deletion.
     */
    fun deleteFile(fileName: String)

    /**
     * Performs registered new file creation.
     *
     * Prefer the overload with versioned file modification if it's possible
     */
    fun createFile(fileName: String, content: String)

    /**
     * Performs registered new file creation by copying a revision from
     * the file located by path "[fileName].[version]" in the module sources directory.
     */
    fun createFile(fileName: String, version: UInt)

    fun compile(
        forceOutput: LogLevel? = null,
        assertions: context(Module, ScenarioModule) CompilationOutcome.() -> Unit = {},
    )
}