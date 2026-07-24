// TARGET_BACKEND: WASM
// WITH_STDLIB

private const val EXPECTED = "A\uD83D\uDE80\u0000e\u0301Z"

@JsFun("() => \"A\\uD83D\\uDE80\\u0000e\\u0301Z\"")
external fun jsCreateString(): String

@JsFun("""(s) => typeof s === "string" && !(s instanceof String)""")
external fun jsIsPrimitiveString(s: String): Boolean

private fun charArrayString(): String =
    charArrayOf('A', '\uD83D', '\uDE80', '\u0000', 'e', '\u0301', 'Z').concatToString()

fun box(): String {
    val literal = EXPECTED
    val fromJs = jsCreateString()
    val fromCharArray = charArrayString()
    val fromJsString = literal.toJsString().toString()

    val values = listOf(literal, fromJs, fromCharArray, fromJsString)

    for ((index, value) in values.withIndex()) {
        if (value != EXPECTED) return "Fail value $index: <$value>"
        if (value != literal) return "Fail equals $index"
        if (value.hashCode() != literal.hashCode()) return "Fail hash $index"
        if (!jsIsPrimitiveString(value)) return "Fail primitive $index"
    }

    val map = hashMapOf(literal to "OK")
    if (map[fromJs] != "OK") return "Fail map from JS"
    if (map[fromCharArray] != "OK") return "Fail map from char array"
    if (map[fromJsString] != "OK") return "Fail map from JsString"

    return "OK"
}
