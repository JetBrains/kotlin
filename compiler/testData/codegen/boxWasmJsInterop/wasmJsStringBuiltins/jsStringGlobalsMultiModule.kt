// TARGET_BACKEND: WASM
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

package lib

const val LIB_EDGE = "L'\u0000\uD83D\uDE80e\u0301"

fun decorateFromLib(value: String): String {
    val middle = LIB_EDGE.substring(1, LIB_EDGE.length)
    return "[" + middle + ":" + value + "]"
}

// MODULE: main(lib)
// FILE: main.kt

import lib.LIB_EDGE
import lib.decorateFromLib

@JsFun("""(s) => typeof s === "string" && !(s instanceof String)""")
external fun jsIsPrimitiveString(s: String): Boolean

fun box(): String {
    val result = decorateFromLib("OK")
    val expected = "['\u0000\uD83D\uDE80e\u0301:OK]"

    if (result != expected) return "Fail result: <$result>"
    if (!jsIsPrimitiveString(result)) return "Fail result is not primitive"
    if (!jsIsPrimitiveString(LIB_EDGE)) return "Fail const is not primitive"

    return "OK"
}
