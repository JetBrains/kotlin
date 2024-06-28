// FIR_IDENTICAL
// ISSUE: KT-61572

// MODULE: m1-common
// FILE: common.kt

expect open class A {
    open fun foo(x: Int = 20, y: Int = 3): Int
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual open class A {
    actual open fun foo(x: Int, y: Int) = x + y
}

open class B : A() {
    override fun foo(x: Int, y: Int) = 0

    fun bar1() = super.<!SUPER_CALL_WITH_DEFAULT_PARAMETERS!>foo<!>()
}
