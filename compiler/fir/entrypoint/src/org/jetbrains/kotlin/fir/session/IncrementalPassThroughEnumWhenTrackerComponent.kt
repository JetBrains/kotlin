/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirEnumWhenTrackerComponent
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker


class IncrementalPassThroughEnumWhenTrackerComponent(private val enumWhenTracker: EnumWhenTracker) : FirEnumWhenTrackerComponent() {
    override fun report(whenExpressionFilePath: String, enumClassFqName: String) {
        enumWhenTracker.report(whenExpressionFilePath, enumClassFqName)
    }
}