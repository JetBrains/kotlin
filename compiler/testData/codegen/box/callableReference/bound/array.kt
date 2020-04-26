open class A {
    var f: String = "OK"
}

class B : A() {
}

fun box() : String {
    val b = B()
    return (b::f).get()
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
