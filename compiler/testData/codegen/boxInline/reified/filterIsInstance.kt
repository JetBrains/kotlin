// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not

// WITH_STDLIB

// FILE: lib.kt
inline fun<reified T> Array<Any>.filterIsInstance(): List<T> {
    return this.filter { it is T }.map { it as T }
}

// FILE: main.kt
import kotlin.test.assertEquals

fun box(): String {
    val src: Array<Any> = arrayOf(1,2,3.toDouble(), "abc", "cde")

    assertEquals(arrayListOf(1,2), src.filterIsInstance<Int>())
    assertEquals(arrayListOf(3.0), src.filterIsInstance<Double>())
    assertEquals(arrayListOf("abc", "cde"), src.filterIsInstance<String>())
    assertEquals(src.toList(), src.filterIsInstance<Any>())

    return "OK"
}
