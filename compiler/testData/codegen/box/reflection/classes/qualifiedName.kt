// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

class Klass {
    class Nested
    companion object
}

fun box(): String {
    assertEquals("Klass", Klass::class.qualifiedName)
    assertEquals("Klass.Nested", Klass.Nested::class.qualifiedName)
    assertEquals("Klass.Companion", Klass.Companion::class.qualifiedName)

    class Local
    assertEquals(null, Local::class.qualifiedName)

    val o = object {}
    assertEquals(null, o.javaClass.kotlin.qualifiedName)

    return "OK"
}
