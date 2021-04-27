// IGNORE_BACKEND_FIR: JVM_IR

fun box(): String {
    val a1: Byte? = 1.plus(1)
    val a2: Short? = 1.plus(1)
    val a3: Int? = 1.plus(1)
    val a4: Long? = 1.plus(1)
    val a5: Double? = 1.0.plus(1)
    val a6: Float? = 1f.plus(1)
    val a7: Char? = 'A'.plus(1)
    val a8: Int? = 'B'.minus('A')

    if (a1!! != 2.toByte()) return "fail 1"
    if (a2!! != 2.toShort()) return "fail 2"
    if (a3!! != 2) return "fail 3"
    if (a4!! != 2L) return "fail 4"
    if (a5!! != 2.0) return "fail 5"
    if (a6!! != 2f) return "fail 6"
    if (a7!! != 'B') return "fail 7"
    if (a8!! != 1) return "fail 8"

    return "OK"
}
