/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

// This is an example Lincheck test to ensure that the test infrastructure is working. It will be replaced with proper "cleanable soft value
// cache" tests in the scope of KT-62136.

class ExampleLincheckTest {
    private val c = AtomicInteger(0)

    @Operation
    fun inc(): Int = c.incrementAndGet()

    @Operation
    fun get(): Int = c.get()

    @Test
    fun stressTest() = StressOptions().check(this::class)

    @Test
    fun modelCheckingTest() = ModelCheckingOptions().check(this::class)
}
