// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java

public class J {
    public final String result;

    private J(String result) {
        this.result = result;
    }
}

// FILE: K.kt

import kotlin.reflect.full.*
import kotlin.reflect.jvm.*
import kotlin.test.*

fun box(): String {
    val c = J::class.constructors.single()
    assertFalse(c.isAccessible)
    assertFailsWith(IllegalCallableAccessException::class) { c.callBy(mapOf(c.parameters.single() to "")) }
    c.isAccessible = true
    assertTrue(c.isAccessible)
    val j = c.callBy(mapOf(c.parameters.single() to "OK"))
    return j.result
}
