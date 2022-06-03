// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.reflect.jvm.isAccessible

operator fun Any?.getValue(x: Any?, y: Any?): String {
    return "OK"
}

val s: String by 1

fun box(): String {
    assertEquals(1, ::s.apply { isAccessible = true }.getDelegate())
    return "OK"
}

