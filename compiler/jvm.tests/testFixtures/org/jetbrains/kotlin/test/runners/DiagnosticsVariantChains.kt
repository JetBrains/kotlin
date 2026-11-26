/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.analysis.test.data.manager.TestVariantChain
import org.jetbrains.kotlin.test.utils.CONTEXT_SENSITIVE_VARIANT

/**
 * The variant chains of the compiler tests over the diagnostic test data besides the golden one, see
 * `compiler/testData/diagnostics/AGENTS.md`. The LL tests have theirs in `LLDiagnosticsVariantChains`.
 */
object DiagnosticsVariantChains {
    val CONTEXT_SENSITIVE: TestVariantChain = listOf(CONTEXT_SENSITIVE_VARIANT)
}
