// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

fun foo(a: String, b: String = "b", c: String, d: String = "d", e: String) =
        a + b + c + d + e

fun box(): String {
    val p = ::foo.parameters
    assertEquals("abcde", ::foo.callBy(mapOf(
            p[0] to "a",
            p[2] to "c",
            p[4] to "e"
    )))

    return "OK"
}
