// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
object Singleton {
    fun ok() = "OK"
}

fun box() = (Singleton::ok)()