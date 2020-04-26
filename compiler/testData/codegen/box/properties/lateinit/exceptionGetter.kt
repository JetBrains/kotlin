class A {
    public lateinit var str: String
}

fun box(): String {
    val a = A()
    try {
        a.str
    } catch (e: RuntimeException) {
        return "OK"
    }
    return "FAIL"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
