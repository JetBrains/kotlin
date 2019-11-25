// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

import kotlin.test.assertEquals

fun checkLongAB5E(x: Long) = assertEquals(0xAB5EL, x)
fun checkDouble1(y: Double) = assertEquals(1.0, y)
fun checkByte10(z: Byte) = assertEquals(10.toByte(), z)

fun box(): String {
    val x = java.lang.Long.valueOf("AB5E", 16)
    checkLongAB5E(x)

    val y = java.lang.Double.valueOf("1.0")
    checkDouble1(y)

    val z = java.lang.Byte.valueOf("A", 16)
    checkByte10(z)

    return "OK"
}
