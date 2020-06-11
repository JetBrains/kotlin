// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.*

class K {
    lateinit var value: String
}

fun box(): String {
    val p = K::value

    assertNotNull(p.javaField, "Fail p field")

    val getter = p.javaGetter!!
    val setter = p.javaSetter!!

    assertEquals(K::class.java.getMethod("getValue"), getter)
    assertEquals(K::class.java.getMethod("setValue", String::class.java), setter)

    assertNull(p.getter.javaConstructor)
    assertNull(p.setter.javaConstructor)

    val k = K()
    assertFails("Fail k getter") { getter.invoke(k) }  // lateinit not yet initialized
    setter.invoke(k, "foo")
    assertEquals("foo", getter.invoke(k), "Fail k setter")

    return "OK"
}
