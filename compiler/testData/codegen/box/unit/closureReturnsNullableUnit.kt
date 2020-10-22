// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
fun isNull(x: Unit?) = x == null

fun box(): String {
    val closure: () -> Unit? = { null }
    if (!isNull(closure())) return "Fail 1"

    return "OK"
}
