// WITH_RUNTIME
// KJS_FULL_RUNTIME
// IGNORE_BACKEND: WASM

interface I {
    companion object {
        val default: IC by lazy(::IC)
    }
}

inline class IC(val ok: String = "OK") : I

fun box(): String {
    return I.default.ok
}