// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class UInt(val s: Int)

fun test(a1: Any, a2: UInt?, a3: Any?, a4: Any?): Int {
    val b1 = a1 as UInt
    val b2 = a2 as UInt
    val b3 = (a3 as UInt?) as UInt
    val b4 = (a4 as? UInt) as UInt
    return b1.s + b2.s + b3.s + b4.s
}

fun box(): String {
    val u1 = UInt(1)
    val u2 = UInt(2)
    if (test(u1, u2, u1, u2) != 6) return "fail"

    return "OK"
}