/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks

import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
abstract class AbstractInferenceBenchmark : AbstractSimpleFileBenchmark() {
    @Param("true", "false")
    private var useNI: Boolean = false

    override val useNewInference: Boolean
        get() = useNI
}