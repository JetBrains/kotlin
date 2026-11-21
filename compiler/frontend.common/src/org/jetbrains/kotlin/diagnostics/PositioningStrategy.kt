/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.KtSourceElement

/**
 * Base for parse-tree agnostic positioning strategies, KT-91911
 * Currently it only has implementations for psi and light-tree.
 */
abstract class PositioningStrategy {
    abstract fun markDiagnostic(diagnostic: KtDiagnosticWithSource): List<TextRange>

    @DiagnosticLossRisk
    abstract fun isValid(element: KtSourceElement): Boolean
}

/**
 * Overriding `isValid` can lead to a loss of diagnostics in tests.
 *
 * Note that it's only called in tests. In the real world, the diagnostics are still reported.
 *
 * Instead of filtering diagnostics using `isValid`, the checker should be adapted.
 */
@RequiresOptIn
annotation class DiagnosticLossRisk
