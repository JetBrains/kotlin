// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT
package test

import kotlin.reflect.full.createType
import kotlin.reflect.KTypeProjection
import kotlin.test.assertEquals

class Foo
class Bar<T>

fun box(): String {
    assertEquals("test.Foo", Foo::class.createType().toString())
    assertEquals("test.Foo?", Foo::class.createType(nullable = true).toString())

    assertEquals("test.Bar<kotlin.String>", Bar::class.createType(listOf(KTypeProjection.invariant(String::class.createType()))).toString())
    assertEquals("test.Bar<kotlin.Int>?", Bar::class.createType(listOf(KTypeProjection.invariant(Int::class.createType())), nullable = true).toString())

    return "OK"
}
