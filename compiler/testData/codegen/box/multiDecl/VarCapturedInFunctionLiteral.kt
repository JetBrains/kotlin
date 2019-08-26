// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class A {
    operator fun component1() = 1
    operator fun component2() = 2
}


fun box() : String {
    var (a, b) = A()

    val local = {
        a = 3
    }
    local()
    return if (a == 3 && b == 2) "OK" else "fail"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
