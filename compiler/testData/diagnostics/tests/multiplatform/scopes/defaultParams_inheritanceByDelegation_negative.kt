// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
interface I {
    fun f(x: Int = 5) = x
}

expect class E : I {
    override fun f(x: Int): Int
}

expect class E2 : I {
    override fun f(x: Int): Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class E(i: I) : I by i

actual class E2(i: I) : I by i {
    actual override fun f(x: Int): Int = x
}
