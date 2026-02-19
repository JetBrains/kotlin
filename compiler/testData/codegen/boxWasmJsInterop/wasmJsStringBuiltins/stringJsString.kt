// TARGET_BACKEND: WASM
// WITH_STDLIB

private val CASES = listOf(
    "",
    "ASCII",
    "Привет",
    "🚀",
    "e\u0301",
    "a\u0000b",
    "שלום"
)

fun box(): String {
    for ((i, s) in CASES.withIndex()) {
        val js: JsString = s.toJsString()
        val back: String = js.toString()
        if (back != s) return "Fail"
    }
    return "OK"
}
