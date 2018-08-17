// WITH_UNSIGNED
// IGNORE_BACKEND: JVM_IR, JS_IR

const val MAX_BYTE: UByte = 0xFFu
const val HUNDRED: UByte = 100u

const val MAX_LONG: ULong = ULong.MAX_VALUE

fun box(): String {
    val maxByteStringSingle = "$MAX_BYTE"
    if (maxByteStringSingle != MAX_BYTE.toString() || maxByteStringSingle != "255") return "Fail 1: $maxByteStringSingle"

    val maxLongStringSingle = "$MAX_LONG"
    if (maxLongStringSingle != MAX_LONG.toString() || maxLongStringSingle != "18446744073709551615") return "Fail 1.5: $maxLongStringSingle"

    val twoHundredUByte = "${(HUNDRED * 2u).toUByte()}"
    if (twoHundredUByte != "200") return "Fail 2: $twoHundredUByte"

    val complexOnlyConstants = "Max: $MAX_BYTE, two hundred: $twoHundredUByte"
    if (complexOnlyConstants != "Max: 255, two hundred: 200") return "Fail 3: $complexOnlyConstants"

    val nonConst = UByte.MAX_VALUE + 1u
    val complex = "Max UByte: $MAX_BYTE, next: $nonConst"
    if (complex != "Max UByte: 255, next: 256") return "Fail 4: $complex"

    return "OK"
}