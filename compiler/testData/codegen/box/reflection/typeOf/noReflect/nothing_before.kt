// !API_VERSION: 1.5
// !OPT_IN: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: box.kt

package test

import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class C<V>(v: V)

fun <T : Nothing?> nothingBound() =
    (typeOf<C<T>>().arguments.single().type!!.classifier as KTypeParameter).upperBounds.single()

fun check(expected: String, actual: KType) {
    assertEquals(expected + " (Kotlin reflection is not available)", actual.toString())
}

fun box(): String {
    check("test.C<java.lang.Void>", typeOf<C<Nothing>>())
    check("test.C<java.lang.Void?>", typeOf<C<Nothing?>>())
    check("java.lang.Void?", nothingBound<Nothing?>())

    check("test.C<java.lang.Void>", returnTypeOf { C(J.platformType<Nothing>()) })

    assertEquals(Void::class, typeOf<C<Nothing>>().arguments.single().type!!.classifier)

    assertEquals(typeOf<C<Nothing>>(), typeOf<C<Void>>())

    return "OK"
}

inline fun <reified Z> returnTypeOf(block: () -> Z) =
    typeOf<Z>()

// FILE: J.java

public class J {
    public static <X> X platformType() { return null; }
}
