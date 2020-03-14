// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

fun join(vararg strings: String) = strings.toList().joinToString("")

fun box(): String {
    val f = ::join
    assertEquals("", f.callBy(emptyMap()))
    return "OK"
}
