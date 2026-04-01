// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*

class A(private var result: String)

fun box(): String {
    val a = A("abc")

    val p = A::class.declaredMemberProperties.single() as KMutableProperty1<A, String>
    p.isAccessible = true
    assertEquals("abc", p.call(a))
    assertEquals(Unit, p.setter.call(a, "def"))
    assertEquals("def", p.getter.call(a))

    return "OK"
}
