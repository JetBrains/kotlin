// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME

import kotlin.test.assertEquals

inline fun <reified T> foo(): T {
    return T::class.java.getName() as T
}

fun box(): String {
    val fooCall = foo() as String
    assertEquals("java.lang.String", fooCall)

    val safeFooCall = foo() as? String
    assertEquals("java.lang.String", safeFooCall)

    return "OK"
}
