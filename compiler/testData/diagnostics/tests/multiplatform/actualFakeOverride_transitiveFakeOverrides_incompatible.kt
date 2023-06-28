// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    fun foo(param: Int)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> : A

interface A : B {
    override fun foo(param: Int) {}
}

interface B {
    fun foo(param: Int = 3) {}
}
