// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_GENERATED
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

class Wrapper<T>(val value: T)

fun box(): String {
    val ls = listOf("OK").map(::Wrapper)
    return ls[0].value
}
