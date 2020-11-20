// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
data class Station(
        val id: String?,
        val name: String,
        val distance: Int)

fun box(): String {
    var result = ""
    // See KT-14399
    listOf(Station("O", "K", 56)).forEachIndexed { i, (id, name, distance) -> result += "$id$name$distance" }
    if (result != "OK56") return "fail: $result"
    return "OK"
}
