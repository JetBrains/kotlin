// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +JvmSupportRecursiveTypeOf

package test

import kotlin.reflect.typeOf
import kotlin.reflect.KTypeParameter
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class C<T: Comparable<U>, U: Comparable<T>> {
    fun makeTU(): List<KTypeParameter> = typeOf<Pair<T, U>>().arguments.map { it.type!!.classifier as KTypeParameter  }
}

fun box(): String {
    val (t, u) = C<Int, Int>().makeTU()
    val tUpper = t.upperBounds.single()
    val uUpper = u.upperBounds.single()
    assertEquals(tUpper.toString(), "kotlin.Comparable<U>")
    assertEquals(uUpper.toString(), "kotlin.Comparable<T>")
    assertEquals(tUpper.arguments.single().type!!.classifier as KTypeParameter, u)
    assertEquals(uUpper.arguments.single().type!!.classifier as KTypeParameter, t)
    return "OK"
}
