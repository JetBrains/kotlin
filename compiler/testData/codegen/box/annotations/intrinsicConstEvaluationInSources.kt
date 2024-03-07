// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6, WASM, NATIVE
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6, WASM, NATIVE
//   non-jvm backends are ignored because of KT-66432
// ISSUE: KT-65415, KT-66432

// FILE: IntrinsicConstEvaluation.kt
package kotlin.internal

annotation class IntrinsicConstEvaluation

// FILE: usage.kt
import kotlin.internal.IntrinsicConstEvaluation

@IntrinsicConstEvaluation
fun test(): String = "OK"

fun box(): String = test()
