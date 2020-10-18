// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    var result = ""
    for (x in listOf('O', 'A', 'K').filter { it > 'D' }) {
        result += object { fun run() = x }.run()
    }
    return result
}
