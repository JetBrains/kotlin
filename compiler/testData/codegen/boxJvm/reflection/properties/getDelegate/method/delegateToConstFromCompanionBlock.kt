// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocks

import kotlin.test.assertEquals
import kotlin.reflect.jvm.isAccessible

operator fun Any?.getValue(x: Any?, y: Any?): String {
    return "OK"
}

class A {
    companion {
        val s: String by 1
    }
}

fun box(): String {
    assertEquals(1, (A::s).apply { isAccessible = true }.getDelegate())

    return "OK"
}
