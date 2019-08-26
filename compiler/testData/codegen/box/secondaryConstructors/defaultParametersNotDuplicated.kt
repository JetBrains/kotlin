// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
var global = 0

fun sideEffect() = global++

class A(val x: String) {
    constructor(y: Int = sideEffect(), z: (Int) -> Int = { it + sideEffect() }) : this("$y:${z(y)}") {}
}

fun box(): String {
    var a = A()
    if (a.x != "0:1") return "failed1: ${a.x}"
    if (global != 2) return "failed2: ${global}"

    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
