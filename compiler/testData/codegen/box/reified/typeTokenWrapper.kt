// TARGET_BACKEND: JVM
// ISSUE: KT-53761
// WITH_STDLIB
// IGNORE_BACKEND: ANDROID

import kotlin.test.assertEquals

open class TypeToken<T> {
    val type = javaClass.genericSuperclass
}

inline fun <reified E> myTypeOf() =
    object : TypeToken<E>() {}.type

inline fun <reified T> myTypeOfArrayOf() =
    object : TypeToken<Array<T>>() {}.type

inline fun <reified T> myTypeOfArrayOf2() =
    myTypeOf<Array<T>>()

inline fun <reified T> myTypeOfListOf() =
    object : TypeToken<List<T>>() {}.type

inline fun <reified T> myTypeOfListOf2() =
    myTypeOf<List<T>>()

fun box(): String {
    assertEquals(myTypeOf<Array<String>>(), myTypeOfArrayOf<String>())
    assertEquals(myTypeOf<Array<String>>(), myTypeOfArrayOf2<String>())
    assertEquals(myTypeOf<List<String>>(), myTypeOfListOf<String>())
    assertEquals(myTypeOf<List<String>>(), myTypeOfListOf2<String>())
    return "OK"
}
