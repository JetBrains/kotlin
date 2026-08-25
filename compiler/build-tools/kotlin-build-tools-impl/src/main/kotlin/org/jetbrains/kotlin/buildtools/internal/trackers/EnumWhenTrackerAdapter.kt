/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.trackers

import org.jetbrains.kotlin.buildtools.api.jvm.CompilerEnumWhenTracker
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker

internal class EnumWhenTrackerAdapter(private val tracker: CompilerEnumWhenTracker) : EnumWhenTracker {
    override fun report(whenExpressionFilePath: String, enumClassFqName: String) {
        tracker.report(whenExpressionFilePath, enumClassFqName)
    }
}
