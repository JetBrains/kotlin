// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun isZero(x: Int) = when(x) {
    0 -> true
    else -> throw Exception()
}

fun box(): String {
    try {
	isZero(1)
    }
    catch (e: Exception) {
        return "OK"
    }
    return "Fail"
}
