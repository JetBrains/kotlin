// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

fun box(): String {
    run {
        test("ok")
        test("ok", 200)
    }
    test("ok")
    test("ok", 300)

    return "OK"
}

private fun test(arg1: String, default: Int = 0) = Unit

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ run 
