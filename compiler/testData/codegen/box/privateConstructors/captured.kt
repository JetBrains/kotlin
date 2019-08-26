// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
public open class Outer private constructor(val s: String) {

    companion object {
        fun test () =  { Outer("OK") }()
    }
}

fun box(): String {
    return Outer.test().s
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
