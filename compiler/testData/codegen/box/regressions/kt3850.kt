// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
private class One {
    val a1 = arrayOf(
            object { val fy = "text"}
    )
}

fun box() = if (One().a1[0].fy == "text") "OK" else "fail"
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ARRAYS
