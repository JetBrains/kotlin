// IGNORE_BACKEND_FIR: JVM_IR
// Note: does not pass on FIR because of non-prohibited Kotlin synthetic properties,
// fun getS() = s is considered to be recursive here :(
// It's a question to be discussed in Dec 2019. Muted at this moment.
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
