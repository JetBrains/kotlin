// Will be executed on JDK 9, 11, 15
// IGNORE_BACKEND: ANDROID
// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: FLAKY

fun test(z: Int): String {
    val result = "" + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() +
            z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() +
            z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() +
            z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() +
            z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() +
            z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() +
            z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 1.toChar() + z + 2.toChar()


    return result
}

fun box(): String {
    val result = test(0)

    if (result.length != 280)
        return "fail 1: ${result.length}"

    return if (result != "0\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u0002")
        "fail 2: ${result}"
    else "OK"
}