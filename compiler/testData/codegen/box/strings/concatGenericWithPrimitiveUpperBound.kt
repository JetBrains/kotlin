// KOTLIN_CONFIGURATION_FLAGS: STRING_CONCAT=inline
// WITH_STDLIB

// IGNORE_BACKEND: WASM
//  ^   wasm-function[2283]:0x218cc: RuntimeError: wasm exception

import kotlin.test.assertEquals

fun <T : Boolean> concatBoolean(x: T) = "[[$x]]"
fun <T : Byte> concatByte(x: T) = "[[$x]]"
fun <T : Short> concatShort(x: T) = "[[$x]]"
fun <T : Int> concatInt(x: T) = "[[$x]]"
fun <T : Long> concatLong(x: T) = "[[$x]]"
fun <T : Float> concatFloat(x: T) = "[[$x]]"
fun <T : Double> concatDouble(x: T) = "[[$x]]"

fun box(): String {
    assertEquals("[[true]]", concatBoolean(true))
    assertEquals("[[0]]", concatByte(0.toByte()))
    assertEquals("[[1]]", concatShort(1.toShort()))
    assertEquals("[[2]]", concatInt(2))
    assertEquals("[[3]]", concatLong(3L))
    assertEquals("[[4.4]]", concatFloat(4.4f))
    assertEquals("[[5.5]]", concatFloat(5.5f))

    return "OK"
}

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 INVOKEVIRTUAL java/lang/StringBuilder\.append \(Z\)Ljava/lang/StringBuilder;
// 3 INVOKEVIRTUAL java/lang/StringBuilder\.append \(I\)Ljava/lang/StringBuilder;
// 1 INVOKEVIRTUAL java/lang/StringBuilder\.append \(J\)Ljava/lang/StringBuilder;
// 1 INVOKEVIRTUAL java/lang/StringBuilder\.append \(F\)Ljava/lang/StringBuilder;
// 1 INVOKEVIRTUAL java/lang/StringBuilder\.append \(D\)Ljava/lang/StringBuilder;
