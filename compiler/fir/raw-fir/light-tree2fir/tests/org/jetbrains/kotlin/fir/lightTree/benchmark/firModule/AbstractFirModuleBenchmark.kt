/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.benchmark.firModule

import org.jetbrains.kotlin.fir.lightTree.benchmark.*
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.LightTree2FirGenerator
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.Psi2FirGenerator
import org.jetbrains.kotlin.fir.lightTree.benchmark.generators.TreeGenerator

abstract class AbstractFirModuleBenchmark :
    AbstractBenchmarkForGivenPath(
        System.getProperty("user.dir") + "/compiler/fir",
        true
    )

open class LightTree2FirBenchmarkFirModule(override val generator: TreeGenerator = LightTree2FirGenerator()) :
    AbstractFirModuleBenchmark()

open class Psi2FirBenchmarkFirModule(override val generator: TreeGenerator = Psi2FirGenerator()) :
    AbstractFirModuleBenchmark()
