// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.full.createType
import kotlin.reflect.KTypeProjection
import kotlin.test.assertEquals

class Foo
class Bar<T>

fun box(): String {
    assertEquals("Foo", Foo::class.createType().toString())
    assertEquals("Foo?", Foo::class.createType(nullable = true).toString())

    assertEquals("Bar<kotlin.String>", Bar::class.createType(listOf(KTypeProjection.invariant(String::class.createType()))).toString())
    assertEquals("Bar<kotlin.Int>?", Bar::class.createType(listOf(KTypeProjection.invariant(Int::class.createType())), nullable = true).toString())

    return "OK"
}
