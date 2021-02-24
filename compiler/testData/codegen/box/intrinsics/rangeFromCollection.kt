// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val list = ArrayList<IntRange>()
    list.add(1..3)
    list[0].start
    return "OK"
}
