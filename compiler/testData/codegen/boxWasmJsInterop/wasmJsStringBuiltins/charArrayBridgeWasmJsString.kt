// TARGET_BACKEND: WASM
// WITH_STDLIB

@JsFun("""(s) => s.length""")
external fun jsLength(s: String): Int

@JsFun("""(s, index) => s.charCodeAt(index)""")
external fun jsCharCodeAt(s: String, index: Int): Int

private fun check(actual: String, expected: String, label: String): String? =
    if (actual == expected) null else "Fail $label: <$actual>"

fun box(): String {
    val chars = charArrayOf('a', '\uD83D', '\uDE80', '\u0000', 'e', '\u0301', 'z')
    val s = chars.concatToString()

    check(s, "a\uD83D\uDE80\u0000e\u0301z", "concatToString")?.let { return it }
    if (s.length != chars.size) return "Fail Kotlin length: ${s.length}"
    if (jsLength(s) != chars.size) return "Fail JS length: ${jsLength(s)}"

    for (index in chars.indices) {
        val actual = jsCharCodeAt(s, index)
        if (actual != chars[index].code) return "Fail charCodeAt $index: $actual"
    }

    val destination = CharArray(8) { '_' }
    val copied = s.toCharArray(destination, 1, 1, 6).concatToString()
    val expectedCopy = charArrayOf('_', '\uD83D', '\uDE80', '\u0000', 'e', '\u0301', '_', '_').concatToString()
    check(copied, expectedCopy, "toCharArray subrange")?.let { return it }

    return "OK"
}
