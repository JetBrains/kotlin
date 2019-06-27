// WITH_RUNTIME
// WITH_REFLECT

import kotlin.test.assertEquals

fun <T, R> foo(x: T): R = TODO()

inline fun <reified T, reified R> bar(x: T, y: R, f: (T) -> R, tType: String, rType: String): Pair<T, R?> {
    assertEquals(tType, T::class.simpleName)
    assertEquals(rType, R::class.simpleName)
    return Pair(x, y)
}

data class Pair<A, B>(val a: A, val b: B)

fun box(): String {
    bar(1, "", ::foo, "Int", "String")

    val s1: Pair<Int, String?> = bar(1, "", ::foo, "Int", "String")
    val (a: Int, b: String?) = bar(1, "", ::foo, "Int", "String")

    val ns: String? = null
    bar(ns, ns, ::foo, "String", "String")

    val s2: Pair<Int?, String?> = bar(null, null, ::foo, "Int", "String")

    return "OK"
}
