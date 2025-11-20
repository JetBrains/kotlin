// WITH_STDLIB
// WITH_REFLECT


import kotlin.test.*
import kotlin.reflect.*

fun <T> foo() = typeOf<List<T>>()

fun box(): String {
    val l = foo<Int>()
    assertEquals(List::class, l.classifier)
    val t = l.arguments.single().type!!.classifier
    assertTrue(t is KTypeParameter)
    assertFalse((t as KTypeParameter).isReified)
    assertEquals("T", (t as KTypeParameter).name)

    return "OK"
}
