// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect class E {
    fun f(x: Int): Int
}

expect class E2 {
    fun f(x: Int): Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
interface I {
    fun f(x: Int = 5): Int = x
}

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>E<!>(i: I) : I by i

actual class E2(i: I) : I by i {
    actual override fun f<!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>(x: Int)<!> = x
}
