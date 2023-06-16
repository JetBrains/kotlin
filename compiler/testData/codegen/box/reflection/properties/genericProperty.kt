// TARGET_BACKEND: JVM

// WITH_REFLECT
package test

import kotlin.test.assertEquals

data class Box<T>(val element: T)

fun box(): String {
    val p = Box<String>::element
    assertEquals("val test.Box<T>.element: T", p.toString())
    return p.call(Box("OK"))
}
