// DUMP_IR
// WASM_IGNORE_FOR: vm=WasmEdge
// DUMP_IR_DIFFERENCE: JS_IR, JS_IR_ES6

fun box(): String {
    var variable = 0
    try {
        try {
            null!!
        } finally {
            variable--
        }
    } catch (e: NullPointerException) {
        return if (variable == -1) "OK" else "Fail"
    }
}
