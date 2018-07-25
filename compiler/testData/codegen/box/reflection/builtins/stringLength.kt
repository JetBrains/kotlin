// IGNORE_BACKEND: JS_IR, JS, NATIVE
// WITH_REFLECT

import kotlin.test.assertEquals

fun box(): String {
    String::class.members
    assertEquals(2, String::length.call("OK"))
    return "OK"
}
