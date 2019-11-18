// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.test.assertEquals

class Foo<K, V>

fun box(): String {
    val foo = Foo::class.starProjectedType
    assertEquals(Foo::class, foo.classifier)
    assertEquals(listOf(KTypeProjection.STAR, KTypeProjection.STAR), foo.arguments)
    assertEquals(foo, Foo::class.createType(listOf(KTypeProjection.STAR, KTypeProjection.STAR)))

    assertEquals(String::class, String::class.starProjectedType.classifier)
    assertEquals(listOf(), String::class.starProjectedType.arguments)

    val tp = Foo::class.typeParameters.first()
    assertEquals(tp.createType(), tp.starProjectedType)

    return "OK"
}
