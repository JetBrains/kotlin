// JVM_ABI_K1_K2_DIFF: <explaining>
// ISSUE: KT-72356
// STOP_EVALUATION_CHECKS
// FILE: A.kt
annotation class A(val x: String = "12345678")

annotation class Something

// FILE: B.kt
open class B { @A fun foo() {} }

// FILE: D.kt
class D {                          @Something fun bar() {} }

class E : B()

fun box(): String {
    return "OK"
}
