// ISSUE: KT-72356
// IGNORE_BACKEND_K2_MULTI_MODULE: JS_IR, WASM_JS
// ^^^ These tests create modules that break FIR dump
// STOP_EVALUATION_CHECKS

// FILE: Something.kt
annotation class A(val x: String)

annotation class Something

// FILE: C.kt
open class C { fun foo(@A("SomeWord") x: Int) {} }

// FILE: D.kt
class D {                 @Something fun bar() {} }

class E : C()

fun box(): String {
    return "OK"
}
