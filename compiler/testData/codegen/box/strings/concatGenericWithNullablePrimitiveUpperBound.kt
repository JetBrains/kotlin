// STRING_CONCAT: inline
// WITH_STDLIB

import kotlin.test.assertEquals

fun <T : Boolean?> concatNBoolean(x: T) = "[[$x]]"
fun <T : Byte?> concatNByte(x: T) = "[[$x]]"
fun <T : Short?> concatNShort(x: T) = "[[$x]]"
fun <T : Int?> concatNInt(x: T) = "[[$x]]"
fun <T : Long?> concatNLong(x: T) = "[[$x]]"
fun <T : Float?> concatNFloat(x: T) = "[[$x]]"
fun <T : Double?> concatNDouble(x: T) = "[[$x]]"

fun box(): String {
    assertEquals("[[true]]", concatNBoolean(true))
    assertEquals("[[0]]", concatNByte(0.toByte()))
    assertEquals("[[1]]", concatNShort(1.toShort()))
    assertEquals("[[2]]", concatNInt(2))
    assertEquals("[[3]]", concatNLong(3L))
    assertEquals("[[4.4]]", concatNFloat(4.4f))
    assertEquals("[[5.5]]", concatNFloat(5.5f))

    return "OK"
}

// CHECK_BYTECODE_TEXT
// 7 INVOKEVIRTUAL java/lang/StringBuilder\.append \(Ljava/lang/Object;\)Ljava/lang/StringBuilder;
