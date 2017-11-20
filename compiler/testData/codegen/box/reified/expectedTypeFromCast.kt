// IGNORE_BACKEND: JS, NATIVE

// LANGUAGE_VERSION: 1.2
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
