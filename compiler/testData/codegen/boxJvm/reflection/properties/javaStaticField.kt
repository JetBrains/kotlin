// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java

public class J {
    public static final String X = "X";
    public static String Y = "Y";
}

// FILE: K.kt

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.reflect.*
import kotlin.reflect.full.isSubtypeOf

fun checkFlexibleString(type: KType) {
    assertEquals(String::class, type.classifier)
    assertEquals(emptyList(), type.arguments)
    assertFalse(type.isMarkedNullable)
    assertTrue(type.isSubtypeOf(typeOf<String>()))
    assertTrue(type.isSubtypeOf(typeOf<String?>()))
}

private fun check(p: KProperty0<*>, isMutable: Boolean, name: String) {
    assertEquals(name, p.name)
    assertEquals(emptyList(), p.parameters)
    assertEquals(emptyList(), p.typeParameters)
    checkFlexibleString(p.returnType)
    assertEquals(KVisibility.PUBLIC, p.visibility)

    assertTrue(p.isFinal)
    assertFalse(p.isOpen)
    assertFalse(p.isAbstract)
    assertFalse(p.isSuspend)
    assertFalse(p.isLateinit)
    assertEquals(!isMutable, p.isConst)

    assertEquals(null, p.getDelegate())

    val getter = p.getter
    assertEquals("<get-$name>", getter.name)
    assertEquals(emptyList(), getter.parameters)
    assertEquals(emptyList(), getter.typeParameters)
    checkFlexibleString(getter.returnType)
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

    assertEquals(name, p.get())
    assertEquals(name, p.invoke())
    assertEquals(name, p.call())
    assertEquals(name, p.callBy(emptyMap()))
    assertEquals(name, getter())
    assertEquals(name, getter.call())
    assertEquals(name, getter.callBy(emptyMap()))

    if (isMutable) {
        val setter = (p as KMutableProperty0<String>).setter

        assertEquals("<set-$name>", setter.name)
        assertEquals(emptyList(), setter.typeParameters)
        assertEquals(typeOf<Unit>(), setter.returnType)
        assertEquals(KVisibility.PUBLIC, setter.visibility)

        assertTrue(setter.isFinal)
        assertFalse(setter.isOpen)
        assertFalse(setter.isAbstract)
        assertFalse(setter.isSuspend)
        assertFalse(setter.isInline)
        assertFalse(setter.isExternal)
        assertFalse(setter.isOperator)
        assertFalse(setter.isInfix)

        val param = setter.parameters.single()
        assertEquals(null, param.name)
        checkFlexibleString(param.type)
        assertEquals(0, param.index)
        assertEquals(KParameter.Kind.VALUE, param.kind)
        assertFalse(param.isOptional)
        assertFalse(param.isVararg)

        assertEquals(p, setter.property)
        assertEquals("setter of $p", setter.toString())

        p.set("set")
        assertEquals("set", p.get())
        setter("setter.invoke")
        assertEquals("setter.invoke", p.get())
        setter.call("setter.call")
        assertEquals("setter.call", p.get())
        setter.callBy(mapOf(p.setter.parameters.single() to "setter.callBy"))
        assertEquals("setter.callBy", p.get())
        p.set(name)
    } else {
        assertFalse(p is KMutableProperty0<*>)
    }
}

private fun assertAreEqual(a: Any, b: Any) {
    assertEquals(a, b)
    assertEquals(b, a)
    assertEquals(a.hashCode(), b.hashCode())
    assertEquals(a.toString(), b.toString())
}

fun box(): String {
    val x1 = J::X
    check(x1, false, "X")
    val x2 = J::class.members.single { it.name == "X" } as KProperty0<*>
    check(x2, false, "X")
    assertAreEqual(x1, x2)
    assertEquals("val X: kotlin.String!", x1.toString())

    val y1 = J::Y
    check(y1, true, "Y")
    val y2 = J::class.members.single { it.name == "Y" } as KProperty0<*>
    check(y2, true, "Y")
    assertAreEqual(y1, y2)
    assertEquals("var Y: kotlin.String!", y1.toString())

    return "OK"
}
