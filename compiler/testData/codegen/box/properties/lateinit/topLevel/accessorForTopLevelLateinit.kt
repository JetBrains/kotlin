// IGNORE_BACKEND: JS_IR
// LANGUAGE_VERSION: 1.2

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