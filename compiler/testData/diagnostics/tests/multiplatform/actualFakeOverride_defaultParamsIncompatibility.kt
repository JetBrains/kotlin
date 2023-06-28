// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class A {
    fun foo(a: Int): Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

interface FooProvider {
    fun foo(a: Int = 2): Int = 42
}

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>A<!> : FooProvider
