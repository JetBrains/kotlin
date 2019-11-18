// IGNORE_BACKEND_FIR: JVM_IR
fun <T> assertEquals(a: T, b: T) {
    if (a != b) throw AssertionError("$a != $b")
}

fun box(): String {
    val bytePos = 128.toByte() // Byte.MAX_VALUE + 1
    assertEquals(-128, bytePos.toInt()) // correct, wrapped to Byte.MIN_VALUE

    val shortPos = 32768.toShort() // Short.MAX_VALUE + 1
    assertEquals(-32768, shortPos.toInt()) // correct, wrapped to Short.MIN_VALUE

    assertEquals((-128).toByte().toString(), "-128")
    // TODO: KT-2780
    // assertEquals((-128.toByte()).toString(), "-128")

    return "OK"
}
