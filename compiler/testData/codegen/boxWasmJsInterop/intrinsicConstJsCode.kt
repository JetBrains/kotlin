// TARGET_BACKEND: WASM

// MODULE: lib
// FILE: lib.kt
package constants

const val text = " ok ".trim().uppercase()
const val number = 41.inc()

enum class Mode { FAST }

object Limits {
    const val maxLong = Long.MAX_VALUE.toString()
}

class Holder {
    companion object {
        const val flag = (number > 40).toString()
    }
}

// MODULE: main(lib)
// FILE: main.kt
import constants.text
import constants.number
import constants.Mode
import constants.Limits
import constants.Holder

fun computedJsCode(): Int = js(" Number('".trim() + text + "'.length) + " + (number - 2))

@JsFun(" () => ".trim() + (number - 2) + " + 2")
external fun computedJsFun(): Int

fun doubleJsCode(): String = js("'" + 1.0.toString() + "'")

@JsFun("() => '" + 1.0.toString() + "'")
external fun doubleJsFun(): String

// Kotlin expressions must be evaluated before their text is parsed as JavaScript.
fun intOverflow(): String = js("'" + (Int.MAX_VALUE + 1).toString() + "'")
fun intDivision(): String = js("'" + (7 / 2).toString() + "'")
fun charPlusInt(): String = js("'" + ('a' + 1).toString() + "'")
fun longShift(): String = js("'" + (1L shl 40).toString() + "'")
fun negativeToUInt(): String = js("'" + (-1).toUInt().toString() + "'")
fun maxULong(): String = js("'" + ULong.MAX_VALUE.toString() + "'")
fun bigDouble(): String = js("'" + 1e21.toString() + "'")
fun smallDouble(): String = js("'" + 1.0E-7.toString() + "'")
fun floatToDouble(): String = js("'" + 0.1f.toDouble().toString() + "'")
fun negativeZero(): String = js("'" + (-0.0).toString() + "'")
fun enumName(): String = js("'" + Mode.FAST.name + "'")
fun objectConstant(): String = js("'" + Limits.maxLong + "'")

@JsFun("() => '" + (7 / 2).toString() + "'")
external fun intDivisionJsFun(): String

@JsFun("() => '" + (-0.0).toString() + "'")
external fun negativeZeroJsFun(): String

@JsFun("() => '" + Holder.flag + "'")
external fun companionJsFun(): String

fun box(): String {
    val jsResult = computedJsCode()
    if (jsResult != 42) return "Fail js expression: $jsResult"
    val jsFunResult = computedJsFun()
    if (jsFunResult != 42) return "Fail JsFun: $jsFunResult"

    val results = listOf(
        Triple("doubleJsCode", doubleJsCode(), "1.0"),
        Triple("doubleJsFun", doubleJsFun(), "1.0"),
        Triple("intOverflow", intOverflow(), "-2147483648"),
        Triple("intDivision", intDivision(), "3"),
        Triple("charPlusInt", charPlusInt(), "b"),
        Triple("longShift", longShift(), "1099511627776"),
        Triple("negativeToUInt", negativeToUInt(), "4294967295"),
        Triple("maxULong", maxULong(), "18446744073709551615"),
        Triple("bigDouble", bigDouble(), "1.0E21"),
        Triple("smallDouble", smallDouble(), "1.0E-7"),
        Triple("floatToDouble", floatToDouble(), "0.10000000149011612"),
        Triple("negativeZero", negativeZero(), "-0.0"),
        Triple("enumName", enumName(), "FAST"),
        Triple("objectConstant", objectConstant(), "9223372036854775807"),
        Triple("intDivisionJsFun", intDivisionJsFun(), "3"),
        Triple("negativeZeroJsFun", negativeZeroJsFun(), "-0.0"),
        Triple("companionJsFun", companionJsFun(), "true"),
    )
    for ((name, actualText, expectedText) in results) {
        if (actualText != expectedText) return "Fail $name: expected $expectedText, got $actualText"
    }
    return "OK"
}
