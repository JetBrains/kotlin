// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.full.createType
import kotlin.reflect.KTypeProjection
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Foo<T>

fun box(): String {
    assertEquals(String::class.createType(), String::class.createType())

    assertEquals(
            Foo::class.createType(listOf(KTypeProjection.STAR)),
            Foo::class.createType(listOf(KTypeProjection.STAR))
    )

    val i = Int::class.createType()
    assertEquals(
            Foo::class.createType(listOf(KTypeProjection.invariant(i))),
            Foo::class.createType(listOf(KTypeProjection.invariant(i)))
    )

    assertNotEquals(
            Foo::class.createType(listOf(KTypeProjection.contravariant(i))),
            Foo::class.createType(listOf(KTypeProjection.covariant(i)))
    )

    assertNotEquals(
            Foo::class.createType(listOf(KTypeProjection.covariant(Any::class.createType(nullable = true)))),
            Foo::class.createType(listOf(KTypeProjection.STAR))
    )

    assertNotEquals(
            Foo::class.createType(listOf(KTypeProjection.STAR), nullable = false),
            Foo::class.createType(listOf(KTypeProjection.STAR), nullable = true)
    )

    return "OK"
}
