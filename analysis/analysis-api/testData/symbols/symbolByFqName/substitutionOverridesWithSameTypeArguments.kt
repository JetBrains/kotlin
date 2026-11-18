// callable: test/Derived.foo
// KT-91746
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_DISTINCT_SYMBOL_POINTERS
// DO_NOT_REQUIRE_DISTINCT_NON_PSI_SYMBOL_POINTERS
package test

open class Base<X, Y> {
    fun foo(x: X) {}

    @JvmName("fooY")
    fun foo(x: Y) {}
}

class Derived : Base<String, String>()
