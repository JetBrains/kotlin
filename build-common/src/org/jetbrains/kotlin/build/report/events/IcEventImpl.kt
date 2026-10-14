/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.events

import java.io.Serializable

sealed class IcEventImpl : Serializable {
    val type: String get() = javaClass.simpleName
    val timestamp: Long = System.currentTimeMillis()
    var iteration: Int = UNASSIGNED_ITERATION

    class CompilationStarted(
        val isIncremental: Boolean,
        val reason: String?,
    ) : IcEventImpl()

    class CompileIteration(
        val reasons: Map<String, List<String>>,
        val exitCode: String,
    ) : IcEventImpl()

    companion object {
        const val serialVersionUID: Long = 0L
        const val UNASSIGNED_ITERATION: Int = -1
    }
}
