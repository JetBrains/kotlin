// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT
// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val sb = StringBuilder("OK")
    return "${sb.get(0)}${sb[1]}"
}
