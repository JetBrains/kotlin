// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.test.assertEquals

class Klass

fun box(): String {
    assertEquals("Klass", Klass::class.simpleName)
    assertEquals("Date", java.util.Date::class.simpleName)
    assertEquals("ObjectRef", kotlin.jvm.internal.Ref.ObjectRef::class.simpleName)

    return "OK"
}
