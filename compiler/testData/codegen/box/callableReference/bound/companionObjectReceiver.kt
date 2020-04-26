class A {
    companion object {
        fun ok() = "OK"
    }
}

fun box() = (A.Companion::ok)()
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: BINDING_RECEIVERS
