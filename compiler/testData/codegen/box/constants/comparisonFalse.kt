// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
fun foo(): Array<Boolean> {
    return arrayOf(
        0.0 / 0 == 0.0 / 0,
        0.0F > -0.0F,
        0.0.equals(-0.0),
        (0.0 / 0.0).equals(1.0 / 0.0)
    )
}

fun box(): String {
    if (foo().any { it == true })
        return "fail: ${foo().contentDeepToString()}"
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ any 
