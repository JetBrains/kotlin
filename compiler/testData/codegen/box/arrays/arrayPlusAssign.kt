// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val s = IntArray(1)
    s[0] = 5
    s[0] += 7
    return if (s[0] == 12) "OK" else "Fail ${s[0]}"
}
