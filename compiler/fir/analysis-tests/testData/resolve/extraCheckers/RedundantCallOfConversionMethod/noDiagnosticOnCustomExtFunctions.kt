// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-83441

fun String.toString(param: Boolean): String = if (param) "" else this
fun Double.toDouble(param: Boolean): Double = if (param) 0.0 else this
fun Float.toFloat(param: Boolean): Float = if (param) 0f else this
fun Long.toLong(param: Boolean): Long = if (param) 0L else this
fun Int.toInt(param: Boolean): Int = if (param) 0 else this
fun Char.toChar(param: Boolean): Char = if (param) ' ' else this
fun Short.toShort(param: Boolean): Short = if (param) 0 else this
fun Byte.toByte(param: Boolean): Byte = if (param) 0 else this
fun ULong.toULong(param: Boolean): ULong = if (param) 0uL else this
fun UInt.toUInt(param: Boolean): UInt = if (param) 0u else this
fun UShort.toUShort(param: Boolean): UShort = if (param) 0u else this
fun UByte.toUByte(param: Boolean): UByte = if (param) 0u else this

fun test(param: Boolean): Array<Any> {
    return arrayOf(
        "string".<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toString(param)<!>,
        0.1.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toDouble(param)<!>,
        0.2f.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toFloat(param)<!>,
        3L.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toLong(param)<!>,
        4.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt(param)<!>,
        'c'.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toChar(param)<!>,
        5.toShort().<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toShort(param)<!>,
        6.toByte().<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toByte(param)<!>,
        7UL.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toULong(param)<!>,
        8U.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toUInt(param)<!>,
        9.toShort().<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toShort(param)<!>,
        10.toUShort().<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toUShort(param)<!>,
        11.toByte().<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toByte(param)<!>,
    )
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, ifExpression, integerLiteral, stringLiteral,
thisExpression, unsignedLiteral */
