/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.events

import java.io.Serializable

// Ideally, the basic interface should be in shared bta api folder, while the concrete implementations should be in kotlin-build-statistics

sealed class IcEventImpl(
    val severity: String,
) : Serializable {
    val type: String get() = javaClass.simpleName
    val timestamp: Long = System.currentTimeMillis()
    var iteration: Int = UNASSIGNED_ITERATION

    class CompilationStarted(
        val isIncremental: Boolean,
        val reason: String?,
    ) : IcEventImpl("INFO")

    class CompileIteration(
        val files: List<String>,
        val reasons: Map<String, List<String>>,
        val exitCode: String,
    ) : IcEventImpl("INFO")

    companion object {
        const val serialVersionUID: Long = 0L
        const val UNASSIGNED_ITERATION: Int = -1

        const val NONE = "NONE"
        const val WARNING = "WARNING"
        const val INFO = "INFO"
        const val DEBUG = "DEBUG"
    }
}
