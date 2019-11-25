// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

inline class Z(private val i: Int) {
    fun toByteArray() = ByteArray(1) { i.toByte() }
}

fun box(): String {
    val z = Z(42)
    if (z.toByteArray()[0].toInt() != 42) throw AssertionError()
    return "OK"
}