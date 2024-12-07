// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// FIR status: KT-46419, ILT conversions to Byte and Short are not supported by design

fun box(): String {
    val a1: Byte? = 1.unaryMinus()
    val a2: Short? = 1.unaryMinus()
    val a3: Int? = 1.unaryMinus()
    val a4: Long? = 1.unaryMinus()
    val a5: Double? = 1.0.unaryMinus()
    val a6: Float? = 1f.unaryMinus()

    if (a1!! != (-1).toByte()) return "fail 1"
    if (a2!! != (-1).toShort()) return "fail 2"
    if (a3!! != -1) return "fail 3"
    if (a4!! != -1L) return "fail 4"
    if (a5!! != -1.0) return "fail 5"
    if (a6!! != -1f) return "fail 6"

    return "OK"
}
