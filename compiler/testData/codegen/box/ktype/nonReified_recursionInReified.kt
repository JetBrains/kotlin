// WITH_STDLIB
// WITH_REFLECT
// FILE: lib.kt
import kotlin.reflect.*
inline fun <reified T : Comparable<T>> recursionInReified() = typeOf<List<T>>()

// FILE: main.kt


import kotlin.test.*
import kotlin.reflect.*

fun box(): String {
    val l = recursionInReified<Int>()
    assertEquals(List::class, l.classifier)
    assertEquals(Int::class, l.arguments.single().type!!.classifier)

    return "OK"
}
