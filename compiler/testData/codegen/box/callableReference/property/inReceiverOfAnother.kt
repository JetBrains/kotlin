// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
var x = "OK"

class C(init: () -> String) {
    val value = init()
}

fun box() = C(::x)::value.get()
