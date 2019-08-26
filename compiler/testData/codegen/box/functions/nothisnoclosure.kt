fun foo(x: Int) {}

fun loop(times : Int) {
   var left = times
   while(left > 0) {
        val u : (value : Int) -> Unit = {
            foo(it)
        }
        u(left--)
   }
}

fun box() : String {
    loop(5)
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
