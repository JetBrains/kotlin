// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocksAndExtensions

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
    // TODO: uncomment when KT-85767 is fixed.
    // assertEquals(1, (A::s).apply { isAccessible = true }.getDelegate())

    return "OK"
}
