enum class Empty

fun box(): String {
    if (Empty.values().size != 0) return "Fail: ${Empty.values()}"

    try {
        val found = Empty.valueOf("nonExistentEntry")
        return "Fail: $found"
    }
    catch (e: Exception) {
        return "OK"
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY
