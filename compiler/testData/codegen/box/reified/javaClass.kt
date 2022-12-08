// TARGET_BACKEND: JVM
// IGNORE_INLINER: IR

// WITH_STDLIB

import kotlin.test.assertEquals

inline fun<reified T : Any> javaClassName(): String {
    return T::class.java.getName()
}

fun box(): String {
    assertEquals("java.lang.String", javaClassName<String>())
    assertEquals("java.lang.Integer", javaClassName<Int>())
    assertEquals("java.lang.Object", javaClassName<Any>())
    return "OK"
}
