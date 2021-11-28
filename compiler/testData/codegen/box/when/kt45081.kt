// WITH_STDLIB
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT

fun test(text: String): String {
    when (text.takeWhile { it.isLetter() }) {
        in arrayOf("a") -> return "OK"
    }
    return "FAIL"
}

fun box(): String = test("a")
