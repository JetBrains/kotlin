// WITH_STDLIB
// WITH_REFLECT


import kotlin.test.*
import kotlin.reflect.*

inline fun <reified T : Comparable<T>> recursionInReified() = typeOf<List<T>>()

fun box(): String {
    val l = recursionInReified<Int>()
    assertEquals(List::class, l.classifier)
    assertEquals(Int::class, l.arguments.single().type!!.classifier)

    return "OK"
}
