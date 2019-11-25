// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class UInt(val value: Int)

fun box(): String {
    val a = UInt(123)
    if(a.value != 123) return "fail"

    val c = a.value.hashCode()
    if (c.hashCode() != 123.hashCode()) return "fail"

    val b = UInt(100).value + a.value
    if (b != 223) return "faile"

    return "OK"
}