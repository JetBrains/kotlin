// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KTypeParameter
import kotlin.test.*

class A<U> {
    fun <T> foo(): T = null!!
    fun bar(): Array<U>? = null!!
}

fun box(): String {
    val t = A::class.members.single { it.name == "foo" }.returnType
    assertFalse(t.isMarkedNullable)
    val tc = t.classifier
    if (tc !is KTypeParameter) fail(tc.toString())
    assertEquals("T", tc.name)

    val u = A::class.members.single { it.name == "bar" }.returnType
    assertTrue(u.isMarkedNullable)
    assertEquals(Array<Any>::class, u.classifier)

    return "OK"
}
