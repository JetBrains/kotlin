/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * The kind of parse tree used in the compilation (PSI, LightTree or KMP), with the services specific to it,
 * or [NoParseTreeKind] if diagnostics aren't supposed to be linked to any parse tree.
 * It's provided by [DiagnosticContext.parseTreeKind] to position diagnostics.
 */
abstract class ParseTreeKind {
    abstract val isPsi: Boolean

    abstract val isKmp: Boolean

    /**
     * The implementation of this parse-tree agnostic positioning strategy for the concrete parse tree.
     */
    abstract val SourceElementPositioningStrategy.positioningStrategy: PositioningStrategy
}

object NoParseTreeKind : ParseTreeKind() {
    private const val REASON = "Diagnostics in this context aren't linked to a parse tree"

    override val isPsi: Boolean get() = shouldNotBeCalled(REASON)
    override val isKmp: Boolean get() = shouldNotBeCalled(REASON)
    override val SourceElementPositioningStrategy.positioningStrategy: PositioningStrategy get() = shouldNotBeCalled(REASON)
}
