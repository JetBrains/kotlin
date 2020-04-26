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

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY
