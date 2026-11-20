// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public class J extends Base {}

// FILE: main.kt
package test

import kotlin.reflect.KClass
import kotlin.test.assertEquals

open class Base {
    private val x: String = "Base.x"
    fun getX(): String = x
}

private fun KClass<*>.checkMembers(vararg expected: String) {
    assertEquals(
        expected.toList(),
        members.filter { it.name != "equals" && it.name != "hashCode" && it.name != "toString" }.map { it.toString() }.sorted(),
    )
}

fun box(): String {
    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        return "OK"
    }

    J::class.checkMembers(
        "fun test.J.getX(): kotlin.String",
    )

    return "OK"
}
