// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT
// WITH_RUNTIME

fun box(): String {
    val sb = StringBuilder("NK")
    sb[0]++
    return sb.toString()
}
