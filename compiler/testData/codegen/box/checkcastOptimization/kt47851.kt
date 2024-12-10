// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// FIR status: result.getMethod OK in FE1.0, unresolved in FIR
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

class C(val value: String) {
    fun getField() = value
    fun getMethod() {}
}

fun foo(): Any {
    var result: Any = ""
    result = C("OK")
    try {
        result = result.getField()
    } catch (e: Exception) {
        result.getMethod()
    }
    return result
}


fun box() = foo().toString()
