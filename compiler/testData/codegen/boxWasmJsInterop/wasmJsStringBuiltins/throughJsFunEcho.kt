// TARGET_BACKEND: WASM
// WITH_STDLIB

@JsFun("(s) => s")
external fun jsEcho(s: String): String

private val CASES = listOf(
    "OK",
    "🚀",
    "a\u0000b",
    "e\u0301",
    "Привет🚀"
)

fun box(): String {
    for ((i, s) in CASES.withIndex()) {
        val r = jsEcho(s)
        if (r != s) return "Fail"
    }
    return "OK"
}
