/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark.totalKotlin

import org.jetbrains.kotlin.fir.lightTree.benchmark.*
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.LightTree2FirGenerator
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.Psi2FirGenerator
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.TreeGenerator
import org.openjdk.jmh.annotations.*

abstract class AbstractTotalKotlinBenchmark :
    AbstractBenchmarkForGivenPath(System.getProperty("user.dir"))

open class LightTree2FirTotalKotlinBenchmark(override val generator: TreeGenerator = LightTree2FirGenerator()) :
    AbstractTotalKotlinBenchmark()

open class Psi2FirTotalKotlinBenchmark(override val generator: TreeGenerator = Psi2FirGenerator()) :
    AbstractTotalKotlinBenchmark()