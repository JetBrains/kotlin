// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    foo {
        sb.append(it)
    }
    assertEquals("42", sb.toString())
    return "OK"

}

fun foo(f: (Int) -> Unit) {
    f(42)
}
