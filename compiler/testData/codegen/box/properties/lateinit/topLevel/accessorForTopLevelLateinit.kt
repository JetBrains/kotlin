// IGNORE_BACKEND_FIR: JVM_IR
// FILE: lateinit.kt
private lateinit var s: String

object C {
    fun setS(value: String) { s = value }
    fun getS() = s
}

// FILE: test.kt
fun box(): String {
    C.setS("OK")
    return C.getS()
}
