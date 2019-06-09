/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark.coroutines

import org.jetbrains.kotlin.fir.lightTree.benchmark.AbstractBenchmarkForGivenPath
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.LightTree2FirGenerator
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.Psi2FirGenerator
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.TreeGenerator

abstract class AbstractCoroutinesBenchmark :
    AbstractBenchmarkForGivenPath(System.getProperty("user.dir") + "/compiler/fir/lightTree/testData/coroutines", false)

open class LightTree2FirCoroutinesBenchmark(override val generator: TreeGenerator = LightTree2FirGenerator()) :
    AbstractCoroutinesBenchmark()

open class Psi2FirCoroutinesBenchmark(override val generator: TreeGenerator = Psi2FirGenerator()) :
    AbstractCoroutinesBenchmark()