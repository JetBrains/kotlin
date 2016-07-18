// WITH_REFLECT

import kotlin.reflect.KTypeProjection
import kotlin.reflect.createType
import kotlin.reflect.starProjectedType
import kotlin.test.assertEquals

class Foo<K, V>

fun box(): String {
    val foo = Foo::class.starProjectedType
    assertEquals(Foo::class, foo.classifier)
    assertEquals(listOf(KTypeProjection.Star, KTypeProjection.Star), foo.arguments)
    assertEquals(foo, Foo::class.createType(listOf(KTypeProjection.Star, KTypeProjection.Star)))

    assertEquals(String::class, String::class.starProjectedType.classifier)
    assertEquals(listOf(), String::class.starProjectedType.arguments)

    val tp = Foo::class.typeParameters.first()
    assertEquals(tp.createType(), tp.starProjectedType)

    return "OK"
}
