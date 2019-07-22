/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark.createdFiles

import org.jetbrains.kotlin.fir.lightTree.benchmark.*
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.LightTree2FirGenerator
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.Psi2FirGenerator
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.TreeGenerator
import org.openjdk.jmh.annotations.*
import java.io.File
import java.util.concurrent.TimeUnit

abstract class AbstractKotlinBenchmark : AbstractBenchmark() {
    @Param(
        /* CLASSES */
        "1Class", "10Classes", "100Classes", "1000Classes", "10_000Classes", "100_000Classes",
        "1Cin1C", "10Cin1C", "100Cin1C", "1000Cin1C", "10_000Cin1C", "100_000Cin1C",
        "1Cin10C", "10Cin10C", "100Cin10C", "1000Cin10C", "10_000Cin10C",
        "1Cin100C", "10Cin100C", "100Cin100C", "1000Cin100C",
        "1Cin1000C", "10Cin1000C", "100Cin1000C",
        "1Cin10_000C", "10Cin10_000C", "1Cin100_000C",
        /* FUNCTIONS */
        "1Fun", "10Fun", "100Fun", "1000Fun", "10_000Fun", "100_000Fun",
        "1Fin1C", "1Fin10C", "1Fin100C", "1Fin1000C", "1Fin10_000C", "1Fin100_000C",
        "10Fin1C", "10Fin10C", "10Fin100C", "10Fin1000C", "10Fin10_000C",
        "100Fin1C", "100Fin10C", "100Fin100C", "100Fin1000C",
        /* PROPERTIES */
        "1Var", "10Var", "100Var", "1000Var", "10_000Var", "100_000Var",
        "1Vin1C", "1Vin10C", "1Vin100C", "1Vin1000C", "1Vin10_000C", "1Vin100_000C",
        "10Vin1C", "10Vin10C", "10Vin100C", "10Vin1000C", "10Vin10_000C",
        "100Vin1C", "100Vin10C", "100Vin100C", "100Vin1000C"
    )
    var methodName: String = ""

    private var text: String = ""
    private var file: File = File("$methodName.kt")

    @Setup
    fun setUp() {
        generator.setUp()

        val testMethod = TestCases::class.java.getMethod(methodName)
        text = testMethod.invoke(TestCases()) as String
    }

    @TearDown
    fun tearDown() {
        generator.tearDown()
    }

    @Benchmark
    fun testCreatedFiles() {
        generator.generateFir(text, file, stubMode)
    }
}

open class LightTree2FirKotlinBenchmark(override val generator: TreeGenerator = LightTree2FirGenerator()) :
    AbstractKotlinBenchmark()

open class Psi2FirKotlinBenchmark(override val generator: TreeGenerator = Psi2FirGenerator()) :
    AbstractKotlinBenchmark()