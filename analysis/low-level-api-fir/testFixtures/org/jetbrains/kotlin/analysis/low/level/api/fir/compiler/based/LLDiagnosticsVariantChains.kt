/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.test.utils.LL_FIR_VARIANT
import org.jetbrains.kotlin.test.utils.PARTIAL_BODY_VARIANT
import org.jetbrains.kotlin.test.utils.REVERSED_VARIANT

/**
 * The variant chains of the LL tests over the compiler's diagnostic test data, see `compiler/testData/diagnostics/AGENTS.md`. The compiler
 * tests have theirs in `DiagnosticsVariantChains`.
 */
object LLDiagnosticsVariantChains {
    val LL: TestVariantChain = listOf(LL_FIR_VARIANT)
    val REVERSED: TestVariantChain = LL + REVERSED_VARIANT
    val PARTIAL_BODY: TestVariantChain = LL + PARTIAL_BODY_VARIANT
}
