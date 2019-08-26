// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
var result = ""

fun getReceiver() : Int {
    result += "getReceiver->"
    return 1
}

fun getFun(b : Int.(Int)->Unit): Int.(Int)->Unit {
    result += "getFun()->"
    return b
}

fun box(): String {
    getReceiver().(getFun({ result +="End" }))(1)

    if(result != "getFun()->getReceiver->End") return "fail $result"

    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
