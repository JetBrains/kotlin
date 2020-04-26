fun foo(x: Long, y: Int, z: Double, s: String) {}

fun box(): String {
    while (true) {
        try {
            foo(0, 0, 0.0, "" + continue)
        }
        finally {
            foo(0, 0, 0.0, "" + break)
        }
    }
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
