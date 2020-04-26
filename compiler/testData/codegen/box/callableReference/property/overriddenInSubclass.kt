open class Base {
    open val foo = "Base"
}

class Derived : Base() {
    override val foo = "OK"
}

fun box() = (Base::foo).get(Derived())

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
