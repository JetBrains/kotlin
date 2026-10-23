// TARGET_BACKEND: WASM

// MODULE: lib
// FILE: lib.kt
package constants

const val text = " ok ".trim().uppercase()
const val number = 41.inc()

// MODULE: main(lib)
// FILE: main.kt
import constants.text
import constants.number

fun computedJsCode(): Int = js(" Number('".trim() + text + "'.length) + " + (number - 2))

@JsFun(" () => ".trim() + (number - 2) + " + 2")
external fun computedJsFun(): Int

fun doubleJsCode(): String = js("'" + 1.0 + "'")

@JsFun("() => '" + 1.0 + "'")
external fun doubleJsFun(): String

fun box(): String {
    val jsResult = computedJsCode()
    if (jsResult != 42) return "Fail js expression: $jsResult"
    val jsFunResult = computedJsFun()
    if (jsFunResult != 42) return "Fail JsFun: $jsFunResult"
    val doubleJsResult = doubleJsCode()
    if (doubleJsResult != "1.0") return "Fail Double js expression: $doubleJsResult"
    val doubleJsFunResult = doubleJsFun()
    if (doubleJsFunResult != "1.0") return "Fail Double JsFun: $doubleJsFunResult"
    return "OK"
}
