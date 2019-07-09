// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.full.*
import kotlin.test.*

class A {
    companion object C
}

enum class E {
    ENTRY;
    companion object {}
}

fun box(): String {
    val obj = A::class.companionObject
    assertNotNull(obj)
    assertEquals("C", obj!!.simpleName)

    assertEquals(A.C, A::class.companionObjectInstance)
    assertEquals(A.C, obj.objectInstance)

    assertNull(A.C::class.companionObject)
    assertNull(A.C::class.companionObjectInstance)

    assertEquals(E.Companion, E::class.companionObjectInstance)

    assertEquals(String, String::class.companionObjectInstance)
    assertEquals(String, String.Companion::class.objectInstance)
    assertEquals(Enum, Enum::class.companionObjectInstance)
    assertEquals(Enum, Enum.Companion::class.objectInstance)
    assertEquals(Double, Double::class.companionObjectInstance)
    assertEquals(Double, Double.Companion::class.objectInstance)
    assertEquals(Float, Float::class.companionObjectInstance)
    assertEquals(Float, Float.Companion::class.objectInstance)
    assertEquals(Int, Int::class.companionObjectInstance)
    assertEquals(Int, Int.Companion::class.objectInstance)
    assertEquals(Long, Long::class.companionObjectInstance)
    assertEquals(Long, Long.Companion::class.objectInstance)
    assertEquals(Short, Short::class.companionObjectInstance)
    assertEquals(Short, Short.Companion::class.objectInstance)
    assertEquals(Byte, Byte::class.companionObjectInstance)
    assertEquals(Byte, Byte.Companion::class.objectInstance)
    assertEquals(Char, Char::class.companionObjectInstance)
    assertEquals(Char, Char.Companion::class.objectInstance)

    return "OK"
}
