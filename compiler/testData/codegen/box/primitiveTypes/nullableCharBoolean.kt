// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val c: Char? = 'a'
    if (c!! - 'a' != 0) return "Fail c"

    val b: Boolean? = false
    if (b!!) return "Fail b"

    return "OK"
}
