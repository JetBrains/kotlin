// TARGET_BACKEND: JVM
// WITH_STDLIB

package test

import kotlin.test.assertEquals

fun box(): String {
    assertEquals("java.util.Date", java.util.Date::class.qualifiedName)
    assertEquals("kotlin.jvm.internal.Ref.ObjectRef", kotlin.jvm.internal.Ref.ObjectRef::class.qualifiedName)

    class Local
    assertEquals(null, Local::class.qualifiedName)

    val o = object {}
    assertEquals(null, o.javaClass.kotlin.qualifiedName)

    return "OK"
}
