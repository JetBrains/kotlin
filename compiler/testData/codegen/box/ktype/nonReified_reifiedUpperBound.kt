// WITH_STDLIB
// WITH_REFLECT

import kotlin.test.*
import kotlin.reflect.*

inline fun <reified T, R : T> reifiedUpperBound() = typeOf<List<R>>()

fun box(): String {
    val l = reifiedUpperBound<Any, Any>()
    assertEquals(List::class, l.classifier, "List::class, l.classifier")
    val r = l.arguments.single().type!!.classifier
    assertTrue(r is KTypeParameter, "r is KTypeParameter")
    assertFalse((r as KTypeParameter).isReified, "(r as KTypeParameter).isReified")
    assertEquals("R", (r as KTypeParameter).name, "\"R\", (r as KTypeParameter).name")
    val t = (r as KTypeParameter).upperBounds.single().classifier
    assertTrue(t is KTypeParameter, "t is KTypeParameter")
    assertTrue((t as KTypeParameter).isReified, "(t as KTypeParameter).isReified")
    assertEquals("T", (t as KTypeParameter).name, "\"T\", (t as KTypeParameter).name")

    return "OK"
}
