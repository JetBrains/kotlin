// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val i1: Int = Int.MAX_VALUE
    val i2 = i1 + 1
    if (i2 > i1) return "fail: Int.MAX_VALUE + 1 should overflow to negative."

    val i3: Int = Int.MIN_VALUE
    val i4 = i3 - 1
    if (i4 < i3) return "fail: Int.MIN_VALUE - 1 should overflow to positive."

    return "OK"
}