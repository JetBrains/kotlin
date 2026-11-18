// callable: test/Derived.foo
// KT-91592
package test

open class Base<X> {
    fun foo(x: X) {}
    fun foo(x: String) {}

    val X.foo: Int get() = 0
    val String.foo: Int get() = 0
}

class Derived : Base<String>()
