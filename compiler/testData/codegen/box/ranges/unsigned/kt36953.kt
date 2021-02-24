// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun testBreak() {
    for (i in 0..1) {
        for (j in break downTo 1u) {}
    }
}

fun testReturn() {
    for (i in 0..1) {
        for (j in (return) downTo 1u) {}
    }
}

fun testThrow() {
    try {
        for (i in 0..1) {
            for (j in (throw Exception()) downTo 1u) {
            }
        }
    } catch (e: Exception) {}
}

fun box(): String {
    testBreak()
    testReturn()
    testThrow()
    return "OK"
}