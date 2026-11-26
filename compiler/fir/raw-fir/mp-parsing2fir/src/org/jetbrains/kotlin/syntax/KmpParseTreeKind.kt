/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import org.jetbrains.kotlin.diagnostics.PositioningStrategyKind
import org.jetbrains.kotlin.diagnostics.KmpPositioningStrategies
import org.jetbrains.kotlin.diagnostics.KmpPositioningStrategy

object KmpParseTreeKind : ParseTreeKind() {
    override val isPsi: Boolean = false

    override val isKmp: Boolean = true

    override val PositioningStrategyKind.positioningStrategy: KmpPositioningStrategy
        get() = KmpPositioningStrategies[this]
}
