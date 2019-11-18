// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    val f = "kotlin"::length
    val result = f.get()
    return if (result == 6) "OK" else "Fail: $result"
}
