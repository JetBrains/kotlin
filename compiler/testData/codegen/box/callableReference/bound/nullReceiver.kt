// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
val String?.ok: String
    get() = "OK"

fun box() = (null::ok).get()