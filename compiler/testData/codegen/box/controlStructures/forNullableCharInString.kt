// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val str = "abcd"
    var r = ""
    for (c: Char? in str) {
        r = r + c ?: "?"
    }
    if (r != "abcd") throw AssertionError()

    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: iterator()