// callable: test/Derived.foo
// KT-91592
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
package test

open class Base<X> {
    fun foo(x: X) {}
    fun foo(x: String) {}

    val X.foo: Int get() = 0
    val String.foo: Int get() = 0
}

class Derived : Base<String>()
