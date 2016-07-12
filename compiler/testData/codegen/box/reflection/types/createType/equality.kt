// WITH_REFLECT

import kotlin.reflect.createType
import kotlin.reflect.KTypeProjection
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Foo<T>

fun box(): String {
    assertEquals(String::class.createType(), String::class.createType())

    assertEquals(
            Foo::class.createType(listOf(KTypeProjection.Star)),
            Foo::class.createType(listOf(KTypeProjection.Star))
    )

    val i = Int::class.createType()
    assertEquals(
            Foo::class.createType(listOf(KTypeProjection.Invariant(i))),
            Foo::class.createType(listOf(KTypeProjection.Invariant(i)))
    )

    assertNotEquals(
            Foo::class.createType(listOf(KTypeProjection.In(i))),
            Foo::class.createType(listOf(KTypeProjection.Out(i)))
    )

    assertNotEquals(
            Foo::class.createType(listOf(KTypeProjection.Out(Any::class.createType(nullable = true)))),
            Foo::class.createType(listOf(KTypeProjection.Star))
    )

    assertNotEquals(
            Foo::class.createType(listOf(KTypeProjection.Star), nullable = false),
            Foo::class.createType(listOf(KTypeProjection.Star), nullable = true)
    )

    return "OK"
}
