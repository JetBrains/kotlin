object Singleton {
    fun ok() = "OK"
}

fun box() = (Singleton::ok)()
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: BINDING_RECEIVERS
