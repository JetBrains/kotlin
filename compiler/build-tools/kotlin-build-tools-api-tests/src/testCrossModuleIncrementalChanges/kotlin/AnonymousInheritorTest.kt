/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.SnapshotConfig
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.compile
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.execute
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

/**
 * Test scenarios where type dependency is obscured by an intermediate anonymous type
 */
class AnonymousInheritorTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Recompilation of call site affected by an anonymous object - no-inline version")
    @TestMetadata("ic-scenarios/inline-local-class/inline-anonymous-object-evil/lib")
    fun testAnonymousObjectBaseTypeChangeWithOverloads(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/inline-anonymous-object-evil/lib")
            val app = module(
                "ic-scenarios/inline-local-class/inline-anonymous-object-evil/app",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            lib.changeFile("callable.kt") { it.replace("inline fun", "fun") }

            lib.compile()
            app.compile()

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("SomeClass.kt", "withOverload")

            lib.compile(expectedDirtySet = setOf("SomeClass.kt", "callable.kt"))
            app.compile(expectedDirtySet = setOf())
            app.execute(mainClass = "CallSiteKt", exactOutput = WITH_NEW_LAMBDA_BODY)
        }
    }

    private companion object {
        const val INITIAL_OUTPUT = "42"
        const val WITH_NEW_LAMBDA_BODY = "45"
    }
}
