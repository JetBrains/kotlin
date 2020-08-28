// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.*

class K(var value: Long)

fun box(): String {
    val p = K::value

    assertNotNull(p.javaField, "Fail p field")

    val getter = p.javaGetter!!
    val setter = p.javaSetter!!

    assertEquals(K::class.java.getMethod("getValue"), getter)
    assertEquals(K::class.java.getMethod("setValue", Long::class.java), setter)

    assertNull(p.getter.javaConstructor)
    assertNull(p.setter.javaConstructor)

    val k = K(42L)
    assertEquals(42L, getter.invoke(k), "Fail k getter")
    setter.invoke(k, -239L)
    assertEquals(-239L, getter.invoke(k), "Fail k setter")

    return "OK"
}
