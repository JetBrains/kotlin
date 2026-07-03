// DUMP_IR
// DUMP_IR_DIFFERENCE: JVM
//   K/JVM throws java.lang.NullPointerException, not kotlin.NullPointerException
//   K/JVM throws java.lang.Exception, not kotlin.Exception
// WASM_IGNORE_FOR: vm=WasmEdge

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
