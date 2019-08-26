// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun Any.with(operation :  Any.() -> Any) = operation().toString()

val f = { a : Int -> }

fun box () : String {
    return if(20.with {
        this
    } == "20")
        "OK"
    else
        "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
