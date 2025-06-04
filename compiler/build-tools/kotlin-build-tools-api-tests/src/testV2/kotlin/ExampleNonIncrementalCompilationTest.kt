/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain
import org.junit.jupiter.api.DisplayName
import java.nio.file.Paths

class ExampleNonIncrementalCompilationTest : BaseCompilationTest() {
    @DisplayName("Sample non-incremental compilation test with two modules")
    @DefaultStrategyAgnosticCompilationTest
    fun myTest() = runTest {
        val kt = KotlinToolchain.loadImplementation(this.javaClass.classLoader)
        val compilationOperation = kt.jvm.createJvmCompilationOperation(listOf(Paths.get("abc")), Paths.get("dest"))
        kt.executeOperation(compilationOperation)
    }
}
