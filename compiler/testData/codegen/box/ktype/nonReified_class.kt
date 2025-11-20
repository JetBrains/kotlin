// WITH_STDLIB
// WITH_REFLECT


import kotlin.test.*
import kotlin.reflect.*

class C<T> {
    @OptIn(kotlin.ExperimentalStdlibApi::class)
    fun foo() = typeOf<List<T>>()
}

fun box(): String {
    val l = C<Int>().foo()
    assertEquals(List::class, l.classifier)
    val t = l.arguments.single().type!!.classifier
    assertTrue(t is KTypeParameter)
    assertFalse((t as KTypeParameter).isReified)
    assertEquals("T", (t as KTypeParameter).name)

    return "OK"
}
