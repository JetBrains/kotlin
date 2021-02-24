// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS

fun box(): String {
    var state = 0
    val f = (state++)::toString
    val s1 = f()
    if (s1 != "0") return "fail 1: $s1"
    ++state
    val s2 = f()
    if (s2 != "0") return "fail 2: $s2"
    return "OK"
}