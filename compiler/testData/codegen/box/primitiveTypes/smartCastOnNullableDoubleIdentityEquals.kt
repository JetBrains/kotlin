// WITH_STDLIB
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE
// ISSUE: KT-82557, KT-82694, KT-82695
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:1.9,2.0,2.1,2.2
// ^^^ KT-82557: in 2.2.20 and before, comparisons after smartcast were wrong

import kotlin.test.*

fun box(): String {
    val boxedZero: Double? = 0.0;
    assertFalse(boxedZero === -0.0)

    boxedZero!!
    assertTrue(boxedZero === -0.0)

    return "OK"
}
