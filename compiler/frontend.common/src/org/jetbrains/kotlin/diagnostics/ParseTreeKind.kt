/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

/**
 * The kind of parse tree used in the compilation (PSI, LightTree or KMP), with the services specific to it.
 * It's provided by [DiagnosticBaseContext.parseTreeKind] to position diagnostics.
 */
abstract class ParseTreeKind {
    abstract val isPsi: Boolean

    abstract val isKmp: Boolean

    /**
     * The implementation of this parse-tree agnostic positioning strategy for the concrete parse tree.
     */
    abstract val SourceElementPositioningStrategy.positioningStrategy: PositioningStrategy
}
