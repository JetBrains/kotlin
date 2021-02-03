// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_STRING_BUILDER
// WITH_RUNTIME

fun box(): String {
    val sb = StringBuilder("NK")
    sb[0]++
    return sb.toString()
}
