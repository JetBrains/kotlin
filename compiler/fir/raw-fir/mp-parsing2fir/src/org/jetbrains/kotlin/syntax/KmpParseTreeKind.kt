/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import org.jetbrains.kotlin.diagnostics.PositioningStrategy
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategy

object KmpParseTreeKind : ParseTreeKind() {
    override val isPsi: Boolean = false

    override val isKmp: Boolean = true

    override val SourceElementPositioningStrategy.positioningStrategy: PositioningStrategy
        get() = TODO("KT-91322")
}
