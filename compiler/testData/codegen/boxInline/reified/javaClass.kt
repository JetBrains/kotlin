// TARGET_BACKEND: JVM

// WITH_STDLIB

// FILE: lib.kt
package test
inline fun<reified T : Any> javaClassName(): String {
    return T::class.java.getName()
}

// FILE: main.kt
package test
import kotlin.test.assertEquals

fun box(): String {
    assertEquals("java.lang.String", javaClassName<String>())
    assertEquals("java.lang.Integer", javaClassName<Int>())
    assertEquals("java.lang.Object", javaClassName<Any>())
    return "OK"
}
