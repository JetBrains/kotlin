// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    testChar()
    testNullableChar()
    return "OK"
}

private fun testChar() {
    var sum = ""
    for (ch in '1' until '5') {
        sum = sum + ch
    }
    assertEquals("1234", sum)
}

private fun testNullableChar() {
    var sum = ""
    for (ch: Char? in '1' until '5') {
        sum = sum + (ch ?: break)
    }
    assertEquals("1234", sum)
}
