// TARGET_BACKEND: JVM
// WITH_REFLECT

package test

import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

class A<X>

fun box(): String {
    val nullableAOfInString = A::class.createType(listOf(
        KTypeProjection.contravariant(String::class.createType())
    ), true)
    assertEquals("test.A<? super java.lang.String>", nullableAOfInString.javaType.toString())

    val arrayOfListOfInteger = Array::class.createType(listOf(
        KTypeProjection.invariant(List::class.createType(listOf(
            KTypeProjection.invariant(Int::class.createType())
        )))
    ))
    assertEquals("java.util.List<java.lang.Integer>[]", arrayOfListOfInteger.javaType.toString())

    return "OK"
}
