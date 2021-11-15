// WITH_STDLIB
// KJS_FULL_RUNTIME
// SKIP_MANGLE_VERIFICATION
// IGNORE_BACKEND: WASM

interface I {
    companion object {
        val default: IC by lazy(::IC)
    }
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IC(val ok: String = "OK") : I

fun box(): String {
    return I.default.ok
}