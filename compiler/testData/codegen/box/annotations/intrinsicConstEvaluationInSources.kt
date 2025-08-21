// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6, WASM, NATIVE
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6, WASM, NATIVE
//   non-jvm backends are ignored because of KT-66432
// ISSUE: KT-65415, KT-66432
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR, NATIVE
// ^^^ IrClassSymbolImpl is already bound. Signature: kotlin.internal/IntrinsicConstEvaluation|null[0]. Owner: CLASS ANNOTATION_CLASS name:IntrinsicConstEvaluation modality:OPEN visibility:public superTypes:[kotlin.Annotation]

// FILE: IntrinsicConstEvaluation.kt
package kotlin.internal

annotation class IntrinsicConstEvaluation

// FILE: usage.kt
import kotlin.internal.IntrinsicConstEvaluation

@IntrinsicConstEvaluation
fun test(): String = "OK"

fun box(): String = test()
