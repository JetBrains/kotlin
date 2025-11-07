// ISSUE: KT-72356
// STOP_EVALUATION_CHECKS
// FILE: A.kt
annotation class A(val x: String = "12" + "3")

annotation class Something

// FILE: B.kt
open class B { @A fun foo() {} }

// FILE: D.kt
class D {                          @Something fun bar() {} }

class E : B()

fun box(): String {
    return "OK"
}
