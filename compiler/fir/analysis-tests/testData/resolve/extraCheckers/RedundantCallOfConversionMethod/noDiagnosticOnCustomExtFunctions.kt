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
        "string".toString(param),
        0.1.toDouble(param),
        0.2f.toFloat(param),
        3L.toLong(param),
        4.toInt(param),
        'c'.toChar(param),
        5.toShort().toShort(param),
        6.toByte().toByte(param),
        7UL.toULong(param),
        8U.toUInt(param),
        9.toShort().toShort(param),
        10.toUShort().toUShort(param),
        11.toByte().toByte(param),
    )
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, ifExpression, integerLiteral, stringLiteral,
thisExpression, unsignedLiteral */
