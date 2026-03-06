// ISSUE: KT-72356
// STOP_EVALUATION_CHECKS

// FILE: A.kt
annotation class A(val x: String)

annotation class Something

// FILE: C.kt
open class C { fun foo(a: Int, @A(<!EVALUATED("12345678")!>"12345678"<!>) b: Int) {} }

// FILE: D.kt
class D {                         @Something fun bar() {} }

class E : C()

fun box(): String {
    return "OK"
}
