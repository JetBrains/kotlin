/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.lower.loops.BCEForLoopBodyTransformer
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopBodyTransformer
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import org.jetbrains.kotlin.wasm.config.wasmDisableArrayRangeChecksSafeElimination

/**
 * Wasm-specific for-loop lowering that integrates bounds check elimination (BCE).
 *
 * BCE replaces provably-safe array `get`/`set` calls with unchecked variants
 * (`getWithoutBoundCheck`/`setWithoutBoundCheck`) inside for-loops where the
 * loop bounds guarantee the index is within array bounds.
 *
 * BCE is enabled by default. Use `-Xwasm-disable-array-range-checks-safe-elimination` to turn it off.
 * Only effective when `-Xwasm-enable-array-range-checks` is also enabled;
 * when range checks are globally disabled, BCE has no effect since all
 * `rangeCheck` calls are already eliminated.
 */
class WasmForLoopsLowering(context: WasmBackendContext) : ForLoopsLowering(context) {
    override val loopBodyTransformer: ForLoopBodyTransformer? =
        context.configuration.wasmDisableArrayRangeChecksSafeElimination.ifFalse(::BCEForLoopBodyTransformer)
}
