// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
val String?.ok: String
    get() = "OK"

fun box() = (null::ok).get()