// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

package test

import kotlin.test.assertEquals

class Klass {
    class Nested
    companion object
}

fun box(): String {
    assertEquals("test.Klass", Klass::class.qualifiedName)
    assertEquals("test.Klass.Nested", Klass.Nested::class.qualifiedName)
    assertEquals("test.Klass.Companion", Klass.Companion::class.qualifiedName)

    class Local
    assertEquals(null, Local::class.qualifiedName)

    val o = object {}
    assertEquals(null, o.javaClass.kotlin.qualifiedName)

    return "OK"
}
