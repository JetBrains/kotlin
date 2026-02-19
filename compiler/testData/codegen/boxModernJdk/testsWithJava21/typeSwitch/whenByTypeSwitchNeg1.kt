// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 0 INVOKEDYNAMIC typeSwitch

open class Base
class C1 : Base()
class C2 : Base()
class C3 : Base()

fun test(k1: Base, k2: Base): Int {
    return when {
        k1 is C1 -> 1
        k1 is C2 -> 2
        k2 is C1 -> 3
        k2 is C2 -> 4
        else -> 0
    }
}

fun box(): String {
    if (test(Base(), Base()) != 0) return "0"
    if (test(C1(), C1()) != 1) return "1"
    if (test(C2(), C2()) != 2) return "2"
    if (test(C3(), C1()) != 3) return "3"
    if (test(C3(), C2()) != 4) return "4"

    return "OK"
}