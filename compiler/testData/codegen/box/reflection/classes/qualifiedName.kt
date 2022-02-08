// TARGET_BACKEND: JVM
// WITH_STDLIB

package test

import kotlin.test.assertEquals

class Klass {
    class Nested
    class `Nested$With$Dollars`
    companion object
}

fun box(): String {
    assertEquals("test.Klass", Klass::class.qualifiedName)
    assertEquals("test.Klass.Nested", Klass.Nested::class.qualifiedName)
    assertEquals("test.Klass.Nested\$With\$Dollars", Klass.`Nested$With$Dollars`::class.qualifiedName)
    assertEquals("test.Klass.Companion", Klass.Companion::class.qualifiedName)

    assertEquals("java.util.Date", java.util.Date::class.qualifiedName)
    assertEquals("kotlin.jvm.internal.Ref.ObjectRef", kotlin.jvm.internal.Ref.ObjectRef::class.qualifiedName)

    class Local
    assertEquals(null, Local::class.qualifiedName)

    val o = object {}
    assertEquals(null, o.javaClass.kotlin.qualifiedName)

    return "OK"
}
