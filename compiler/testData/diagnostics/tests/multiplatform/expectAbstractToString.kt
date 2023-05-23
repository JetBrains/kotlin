// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

interface B {
    override fun toString(): String
}

expect value class C(val s: String) : B

expect value class D(val s: String) : B

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

@JvmInline
actual value class C(actual val s: String) : B {
    override fun toString(): String = s
}

@JvmInline
actual value class D(actual val s: String) : B
