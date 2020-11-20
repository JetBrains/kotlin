// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    var x: MutableCollection<Int> = ArrayList()
    x + ArrayList()
    return "OK"
}
