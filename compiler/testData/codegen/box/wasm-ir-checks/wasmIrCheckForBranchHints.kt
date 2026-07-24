// TARGET_BACKEND: WASM
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-JS:2.4
// ^^^ KT-84667 is fixed in 2.4.20-Beta1

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

// Verify that likely() and unlikely() actually emit branch hint annotations in the WASM IR.
// If either became a no-op (identity function), no annotation would be emitted and this check would fail.
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction="<annotation-branch-hint>" inFunction=withBranchHints count=2

import kotlin.wasm.internal.*

fun withBranchHints(x: Int): String {
    if (likely(x > 0)) return "positive"
    if (unlikely(x < 0)) return "negative"
    return "zero"
}

fun box(): String {
    if (withBranchHints(5) != "positive") return "Fail: positive case"
    if (withBranchHints(-5) != "negative") return "Fail: negative case"
    if (withBranchHints(0) != "zero") return "Fail: zero case"
    return "OK"
}
