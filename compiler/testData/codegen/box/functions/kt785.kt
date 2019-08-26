// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class A() {
    var x : Int = 0

    var z = {
        x++
    }
}

fun box() : String {
    val a = A()
    a.z()  //problem is here
    return if (a.x == 1) "OK" else "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
