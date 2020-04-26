fun a(): IntArray? = null

fun b(): Nothing = throw Exception()

fun foo(): IntArray = a() ?: b()


fun box(): String {
    try {
        foo()
    } catch (e: Exception) {
        return "OK"
    }

    return "Fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY
