abstract class Base {
    val result = "OK"
}

class Derived : Base()

fun box(): String {
    return (Base::result).get(Derived())
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
