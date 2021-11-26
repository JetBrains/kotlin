// TARGET_BACKEND: JVM

// WITH_STDLIB

import kotlin.test.assertEquals
import kotlin.test.assertFalse

fun box(): String {
    val x = Int::class.javaPrimitiveType!!.kotlin
    val y = Int::class.javaObjectType.kotlin

    assertEquals(x, y)
    assertEquals(x.hashCode(), y.hashCode())
    assertFalse(x === y)

    return "OK"
}
