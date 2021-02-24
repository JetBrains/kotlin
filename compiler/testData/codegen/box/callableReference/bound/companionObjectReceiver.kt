// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
class A {
    companion object {
        fun ok() = "OK"
    }
}

fun box() = (A.Companion::ok)()