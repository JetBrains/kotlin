// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J<F extends Number> extends K<F> {}

// FILE: box.kt
package test

import kotlin.test.assertEquals

open class K<T> {
    fun foo(x: T & Any): T & Any = x
}

class C<U : Number> : J<U>()

fun box(): String {
    assertEquals("fun test.C<U>.foo(U): U", C::class.members.single { it.name == "foo" }.toString())
    return "OK"
}
