// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt

package test

import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class C<V>(v: V)

fun <T : Nothing?> nothingBound() =
    (typeOf<C<T>>().arguments.single().type!!.classifier as KTypeParameter).upperBounds.single()

fun check(expected: String, actual: KType) {
    assertEquals(expected, actual.toString())
}

private fun cNothing(): C<Nothing> = null!!

fun box(): String {
    check("test.C<kotlin.Nothing>", typeOf<C<Nothing>>())
    check("test.C<kotlin.Nothing?>", typeOf<C<Nothing?>>())
    check("kotlin.Nothing?", nothingBound<Nothing?>())

    check("test.C<kotlin.Nothing!>", returnTypeOf { C(J.platformType<Nothing>()) })

    // Such type's classifier is still Void::class, until KT-15518 is fixed.
    // TODO: support a special KClass instance for Nothing (KT-15518).
    assertEquals(Void::class, typeOf<C<Nothing>>().arguments.single().type!!.classifier)

    assertNotEquals(typeOf<C<Nothing>>(), typeOf<C<Void>>())

    assertEquals(::cNothing.returnType, typeOf<C<Nothing>>())

    return "OK"
}

inline fun <reified Z> returnTypeOf(block: () -> Z) =
    typeOf<Z>()

// FILE: J.java

public class J {
    public static <X> X platformType() { return null; }
}
