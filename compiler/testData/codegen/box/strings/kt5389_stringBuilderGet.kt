// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val sb = StringBuilder("OK")
    return "${sb.get(0)}${sb[1]}"
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_STRING_BUILDER
