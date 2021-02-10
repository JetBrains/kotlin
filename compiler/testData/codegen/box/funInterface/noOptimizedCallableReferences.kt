// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SAM_CONVERSIONS
// NO_OPTIMIZED_CALLABLE_REFERENCES

fun interface P {
    fun get(): String
}

class G(val p: P)

fun f(): String = "OK"

fun box(): String = G(::f).p.get()
