// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val array = intArrayOf(11, 12, 13)
    val p = array.get(0)
    if (p != 11) return "fail 1: $p"

    val stringArray = arrayOf("OK", "FAIL")
    return stringArray.get(0)
}