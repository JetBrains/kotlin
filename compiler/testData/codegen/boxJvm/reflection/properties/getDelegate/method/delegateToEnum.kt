// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.reflect.jvm.isAccessible

enum class E {
    OK, NOT_OK
}

operator fun E.getValue(x: Any?, y: Any?): String = name

val s: String by E.OK

fun box(): String {
    assertEquals(E.OK, ::s.apply { isAccessible = true }.getDelegate())
    return "OK"
}
