// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public enum J {
    J1, J2, J3;
}

// FILE: K.kt
import test.J

import kotlin.enums.enumEntries
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.reflect.*

private fun check(p: KProperty0<*>) {
    val name = "entries"
    assertEquals(name, p.name)
    assertEquals(emptyList(), p.parameters)
    assertEquals(emptyList(), p.typeParameters)
    assertEquals("kotlin.enums.EnumEntries<test.J>", p.returnType.toString())
    assertEquals(KVisibility.PUBLIC, p.visibility)

    assertTrue(p.isFinal)
    assertFalse(p.isOpen)
    assertFalse(p.isAbstract)
    assertFalse(p.isSuspend)
    assertFalse(p.isLateinit)
    assertFalse(p.isConst)

    assertEquals(null, p.getDelegate())

    val getter = p.getter
    assertEquals("<get-$name>", getter.name)
    assertEquals(emptyList(), getter.parameters)
    assertEquals(emptyList(), getter.typeParameters)
    assertEquals("kotlin.enums.EnumEntries<test.J>", getter.returnType.toString())
    assertEquals(KVisibility.PUBLIC, getter.visibility)

    assertTrue(getter.isFinal)
    assertFalse(getter.isOpen)
    assertFalse(getter.isAbstract)
    assertFalse(getter.isSuspend)
    assertFalse(getter.isInline)
    assertFalse(getter.isExternal)
    assertFalse(getter.isOperator)
    assertFalse(getter.isInfix)

    assertEquals(p, getter.property)
    assertEquals("getter of $p", getter.toString())

    val result = enumEntries<test.J>()

    val checkCalls = {
        assertEquals(result, p.get())
        assertEquals(result, p.invoke())
        assertEquals(result, p.call())
        assertEquals(result, p.callBy(emptyMap()))
        assertEquals(result, getter())
        assertEquals(result, getter.call())
        assertEquals(result, getter.callBy(emptyMap()))
    }

    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        try {
            checkCalls()
            throw AssertionError("Call of Java `entries` works unexpectedly: this logic is not implemented in K1-based reflection")
        } catch (e: Throwable) {
            // Expected
        }
    } else {
        checkCalls()
    }

    assertFalse(p is KMutableProperty0<*>)
}

private fun assertAreEqual(a: Any, b: Any) {
    assertEquals(a, b)
    assertEquals(b, a)
    assertEquals(a.hashCode(), b.hashCode())
    assertEquals(a.toString(), b.toString())
}

fun box(): String {
    val entries1 = J::entries
    check(entries1)
    val entries2 = J::class.members.single { it.name == "entries" } as KProperty0<*>
    check(entries2)
    assertAreEqual(entries1, entries2)
    assertEquals("val entries: kotlin.enums.EnumEntries<test.J>", entries1.toString())

    return "OK"
}
