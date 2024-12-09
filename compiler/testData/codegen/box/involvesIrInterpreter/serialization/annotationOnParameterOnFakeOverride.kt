// ISSUE: KT-72356
// STOP_EVALUATION_CHECKS

// FILE: Something.kt
annotation class A(val x: String)

annotation class Something

// FILE: C.kt
open class C { fun foo(@A("Somethin") x: Int) {} }

// FILE: D.kt
class D {                 @Something fun bar() {} }

class E : C()

fun box(): String {
    return "OK"
}
