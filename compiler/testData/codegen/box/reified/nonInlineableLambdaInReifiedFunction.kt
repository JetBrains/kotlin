// TARGET_BACKEND: JVM
// IGNORE_INLINER: IR

// WITH_STDLIB

import kotlin.test.assertEquals

fun foo(block: () -> String) = block()
inline fun<reified T : Any> bar1(x: T): String = foo() {
    T::class.java.getName()
}
inline fun<reified T : Any> bar2(x: T, y: String): String = foo() {
    T::class.java.getName() + "#" + y
}

fun box(): String {

    assertEquals("java.lang.Integer", bar1(1))
    assertEquals("java.lang.String#OK", bar2("abc", "OK"))

    return "OK"
}
