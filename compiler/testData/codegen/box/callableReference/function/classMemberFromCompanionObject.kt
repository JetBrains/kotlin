// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: CLASS_REFERENCES
class C {
    fun OK() {}

    companion object {
        val result = C::OK
    }
}

fun box(): String = C.result.name
