
fun nyCompiler() {
    try {
        return
    }
    catch (e: Exception) {}
    finally {
        try {} catch (e: Exception) {}
    }
}


fun nyCompiler2() {
    try {
        return
    }
    finally {
        try {} catch (e: Exception) {}
    }
}

fun box(): String {
    nyCompiler()
    nyCompiler2()
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
