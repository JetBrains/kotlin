// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: lib.kt
inline fun <reified T> foo(): T {
    return T::class.java.getName() as T
}

// FILE: main.kt
import kotlin.test.assertEquals

fun box(): String {
    val fooCall = foo() as String
    assertEquals("java.lang.String", fooCall)

    val safeFooCall = foo() as? String
    assertEquals("java.lang.String", safeFooCall)

    return "OK"
}
