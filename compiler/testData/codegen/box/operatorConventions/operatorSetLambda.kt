// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// See KT-14999

object Obj {
    var key = ""
    var value = ""

    operator fun set(k: String, v: ((String) -> Unit) -> Unit) {
        key += k
        v { value += it }
    }
}

fun box(): String {
    Obj["O"] = label@{ it("K") }
    return Obj.key + Obj.value
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
