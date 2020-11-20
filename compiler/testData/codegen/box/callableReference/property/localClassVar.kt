// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
fun box(): String {
    class Local {
        var result = "Fail"
    }

    val l = Local()
    (Local::result).set(l, "OK")
    return (Local::result).get(l)
}
