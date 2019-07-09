// !LANGUAGE: +NewInference
// FILE: 1.kt
package test

class Foo<T> {
    inner class Inner<P>(val a: T, val b: P)
}

inline fun <A, B> foo(a: A, b: B, foo: Foo<A>, x: (Foo<A>, A, B) -> Foo<A>.Inner<B>): Foo<A>.Inner<B> = x(foo, a, b)

// FILE: 2.kt

import test.*

fun box(): String {

    val foo = foo<String, String>("O", "K", Foo<String>(), Foo<String>::Inner)
    return foo.a + foo.b
}
