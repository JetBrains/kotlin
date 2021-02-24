// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
fun box(): String {
    var i = 0
    {
        if (1 == 1) {
            i++
        } else {
        }
    }()
    return "OK"
}
