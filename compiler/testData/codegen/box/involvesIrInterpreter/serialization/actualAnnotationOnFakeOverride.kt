// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-72356
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6

// MODULE: common
// FILE: A.kt
annotation class A(val x: String)

annotation class Something

// FILE: B.kt
expect interface B

// FILE: D.kt
class D {               @Something fun bar() {} }

class E : B

// MODULE: platform()()(common)
// FILE: B.kt
actual interface B { @A(<!EVALUATED("12345678")!>"12345678"<!>) fun foo() {} }

fun box(): String {
    return "OK"
}
