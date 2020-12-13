// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IMPLICIT_INTERFACE_METHOD_IMPL

open class B {
    val name: String
        get() = "OK"
}

interface A {
    val name: String
}

class C : B(), A {

}

fun box(): String {
    return C().name
}
