// KT-61572: SUPER_CALL_WITH_DEFAULT_PARAMETERS must be raised for K2/Native in fun bar1()
// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

package foo
expect open class A {
    open fun foo(x: Int = 20, y: Int = 3): Int
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
package foo
actual open class A {
    actual open fun foo(x: Int, y: Int) = x + y
}

open class B : A() {
    override fun foo(x: Int, y: Int) = 0

    fun bar1() = super.foo()
}
