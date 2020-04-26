// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    var s = ""
    for (c in StringBuilder("OK")) {
        s += c
    }
    return s
}


// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_STRING_BUILDER
