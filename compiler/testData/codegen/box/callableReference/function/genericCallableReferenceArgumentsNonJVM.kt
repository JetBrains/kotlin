// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JVM_IR

// WITH_STDLIB
// WITH_REFLECT

import kotlin.test.assertEquals

fun <T, R> foo(x: T): R = TODO()
fun <T> fooReturnInt(x: T): Int = 1

inline fun <reified T, reified R> check(x: T, y: R, f: (T) -> R, tType: String, rType: String) {
    assertEquals(tType, T::class.simpleName)
    assertEquals(rType, R::class.simpleName)
}

inline fun <reified T, reified R> check(f: (T) -> R, g: (T) -> R, tType: String, rType: String) {
    assertEquals(tType, T::class.simpleName)
    assertEquals(rType, R::class.simpleName)
}

fun box(): String {
    check("", 1, ::foo, "String", "Int")
    check("", 1, ::fooReturnInt, "String", "Int")
    check("", "", ::fooReturnInt, "String", "Comparable")  // KT-59348 JVM backends have not "Comparable", but "Any" as common parent for Int and String

    check(Int::toString, ::foo, "Int", "String")

    return "OK"
}
