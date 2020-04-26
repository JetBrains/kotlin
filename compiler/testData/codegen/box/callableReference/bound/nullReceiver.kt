val String?.ok: String
    get() = "OK"

fun box() = (null::ok).get()
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
