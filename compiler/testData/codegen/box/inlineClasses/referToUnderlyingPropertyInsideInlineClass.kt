// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class UInt(val value: Int) {
    operator fun plus(other: UInt) = UInt(value + other.value)
    fun otherValue(other: UInt) = other.value
}

fun box(): String {
    val a = UInt(10)
    val b = UInt(20)
    if (a.otherValue(b) != 20) return "fail 1"

    if ((a + b).value != 30) return "fail 2"

    return "OK"
}