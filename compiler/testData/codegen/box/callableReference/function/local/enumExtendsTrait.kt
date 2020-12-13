// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IMPLICIT_INTERFACE_METHOD_IMPL

interface Named {
    val name: String
}

enum class E : Named {
    OK
}

fun box(): String {
    return E.OK.name
}
