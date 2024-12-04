/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.withMeasure
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker

class RemoteEnumWhenTracker(
    @Suppress("DEPRECATION") val facade: org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade,
    val profiler: Profiler = DummyProfiler()
): EnumWhenTracker {
    override fun report(whenExpressionFilePath: String, enumClassFqName: String) {
        profiler.withMeasure(this) {
            facade.enumWhenTracker_report(whenExpressionFilePath, enumClassFqName)
        }
    }
}