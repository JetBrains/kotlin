// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

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
