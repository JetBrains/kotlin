// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
open class A {
    var f: String = "OK"
}

class B : A() {
}

fun box() : String {
    val b = B()
    return (b::f).get()
}