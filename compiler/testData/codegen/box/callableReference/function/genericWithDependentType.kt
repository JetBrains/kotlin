// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

class Wrapper<T>(val value: T)

fun box(): String {
    val ls = listOf("OK").map(::Wrapper)
    return ls[0].value
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_GENERATED
