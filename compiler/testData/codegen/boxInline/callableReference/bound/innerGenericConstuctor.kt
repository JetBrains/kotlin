// FILE: 1.kt
package test

class Foo<T> {
    inner class Inner<P>(val a: T, val b: P)
}

inline fun <A, B> foo(a: A, b: B, x: (A, B) -> Foo<A>.Inner<B>): Foo<A>.Inner<B> = x(a, b)

// FILE: 2.kt

import test.*

fun box(): String {
    val z = Foo<String>()
    val foo = foo("O", "K", z::Inner)
    return foo.a + foo.b
}
