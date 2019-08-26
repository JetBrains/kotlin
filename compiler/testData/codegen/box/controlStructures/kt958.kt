// IGNORE_BACKEND: WASM
fun test() = 239

fun box() = if(test() in 239..240) "OK" else "fail"

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ .. 
