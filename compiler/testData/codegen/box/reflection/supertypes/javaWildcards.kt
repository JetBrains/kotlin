// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J {
    interface Star extends A<A<?>> {}
    interface Extends<U> extends A<A<? extends U>> {}
    interface Super<V> extends A<A<? super V>> {}

    interface StarBounded extends A<B<?>> {}
    interface ExtendsBounded<W extends Number> extends A<B<? extends W>> {}
    interface SuperBounded<Z> extends A<B<? super Z>> {}
}

// FILE: box.kt
package test

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.test.assertEquals

interface A<T>
interface B<S : Number>

private fun KClass<*>.supertype(): KType =
    supertypes.single { it.classifier != Any::class }

fun box(): String {
    assertEquals("test.A<test.A<*>!>", J.Star::class.supertype().toString())
    assertEquals("test.A<test.A<out U!>!>", J.Extends::class.supertype().toString())
    assertEquals("test.A<test.A<in V!>!>", J.Super::class.supertype().toString())

    assertEquals("test.A<test.B<*>!>", J.StarBounded::class.supertype().toString())
    assertEquals("test.A<test.B<out W!>!>", J.ExtendsBounded::class.supertype().toString())
    assertEquals("test.A<test.B<in Z!>!>", J.SuperBounded::class.supertype().toString())

    return "OK"
}
