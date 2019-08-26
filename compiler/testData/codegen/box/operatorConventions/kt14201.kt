// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
interface Intf {
    val aValue: String
}

class ClassB {
    val x = { "OK" }

    val value: Intf = object : Intf {
        override val aValue = x()
    }
}

fun box() : String {
    return ClassB().value.aValue
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
