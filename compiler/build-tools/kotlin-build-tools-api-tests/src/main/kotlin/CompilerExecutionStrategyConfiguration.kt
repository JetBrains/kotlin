/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.junit.jupiter.api.Assumptions.assumeFalse

typealias CompilerExecutionStrategyConfiguration = Pair<KotlinToolchains, ExecutionPolicy>

val ExecutionPolicy.isDaemonBased: Boolean
    get() = this is ExecutionPolicy.WithDaemon || this is ExecutionPolicy.NativeImageDaemon

/**
 * The native image process execution policy supports JVM compilation only.
 * Non-JVM compilation tests should be skipped instead of failing when this policy is selected.
 */
fun ExecutionPolicy.assumeNotNativeImageProcess() {
    assumeFalse(this is ExecutionPolicy.NativeImageProcess) {
        "The native image process execution policy only supports JVM compilation"
    }
}

fun ExecutionPolicy.assumeIncrementalCompilationIsSupported() {
    assumeFalse(this is ExecutionPolicy.NativeImageProcess) {
        "Incremental compilation is not supported by the native image process strategy"
    }
}

fun ExecutionPolicy.assumeDetailedMetricsAreCollected() {
    assumeFalse(this is ExecutionPolicy.NativeImageProcess) {
        "Detailed compilation metrics are not reported by the native image process strategy"
    }
}
