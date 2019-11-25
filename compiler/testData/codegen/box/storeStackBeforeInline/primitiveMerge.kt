// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import kotlin.test.assertEquals

fun bar() : Boolean = true

fun foobar1(x: Boolean, y: String, z: String) = x.toString() + y + z
fun foobar2(x: Any, y: String, z: String) = x.toString() + y + z

inline fun foo() = "-"

fun box(): String {
    val result1 = foobar1(if (1 == 1) true else bar(), foo(), "OK")
    val result2 = foobar2(if (1 == 1) "true" else arrayOf("false"), foo(), "OK")
    assertEquals("true-OK", result1)
    assertEquals("true-OK", result2)
    return "OK"
}
