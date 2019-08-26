// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class A {
    companion object
}

val foo: Any.() -> Unit = {}

fun test() {
    A.(foo)()
}

fun box(): String {
    test()
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
