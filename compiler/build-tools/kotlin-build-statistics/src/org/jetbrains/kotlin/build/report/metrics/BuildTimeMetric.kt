/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

sealed class BuildTimeMetric private constructor(parent: BuildTimeMetric?, readableString: String, name: String) :
    BuildTime<BuildTimeMetric>(parent, readableString, name) {

    constructor(readableString: String, name: String) : this(null, readableString, name)

    /**
     * Creates a child build-time metric and registers it in this metric's children.
     *
     * Thread-safety: Not thread-safe. This method mutates the underlying children list
     * of BuildTime which is not a thread-safe collection. If accessed from multiple
     * threads, external synchronization is required.
     */
    fun createChild(readableString: String, name: String): BuildTimeMetric {
        return ChildBuildTimeMetric(this, readableString, name).also { children.add(it) }
    }

    private class ChildBuildTimeMetric(
        parent: BuildTimeMetric,
        readableString: String,
        name: String,
    ) : BuildTimeMetric(parent, readableString, name)
}


object JPS_ITERATION : BuildTimeMetric(readableString = "Jps iteration", name = "JPS_ITERATION")

val JPS_COMPILATION_ROUND = JPS_ITERATION.createChild("Sources compilation round", name = "COMPILATION_ROUND")
val JPS_COMPILER_PERFORMANCE = JPS_COMPILATION_ROUND.createChild(readableString = "Compiler time", name = "COMPILER_PERFORMANCE")
val JPS_COMPILER_INITIALIZATION = JPS_COMPILER_PERFORMANCE.createChild("Compiler initialization time", name = "COMPILER_INITIALIZATION")
val JPS_CODE_ANALYSIS = JPS_COMPILER_PERFORMANCE.createChild("Compiler code analysis", name = "CODE_ANALYSIS")
val JPS_CODE_GENERATION = JPS_COMPILER_PERFORMANCE.createChild("Compiler code generation", name = "CODE_GENERATION")

