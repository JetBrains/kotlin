// IGNORE_BACKEND_FIR: JVM_IR
interface Ordinaled {
    val ordinal: Int
}

enum class A : Ordinaled {
    X
}


fun box(): String {
    val result = (A.X as Ordinaled).ordinal

    if (result != 0) return "fail 1: $result"

    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: BRIDGE_ISSUES