// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 3 INVOKEDYNAMIC typeSwitch
// 0 INSTANCEOF

open class Base
class C1 : Base()
class C2 : Base()
class C3 : Base()

fun test(a: Base, b: Base): Int {
    return when (a) {
        is C1 -> when (b) {
            is C1 -> 1
            is C2 -> 2
            else -> 3
        }
        is C2, is C3 -> when (b) {
            is C1 -> 4
            is C2 -> 5
            else -> 6
        }
        else -> 0
    }
}

fun box(): String {
    if (test(Base(), Base()) != 0) return "0"
    if (test(C1(), C1()) != 1) return "1"
    if (test(C1(), C2()) != 2) return "2"
    if (test(C1(), Base()) != 3) return "3"
    if (test(C2(), C1()) != 4) return "4"
    if (test(C2(), C2()) != 5) return "5"
    if (test(C2(), Base()) != 6) return "6"
    if (test(C3(), C1()) != 4) return "7"
    if (test(C3(), C2()) != 5) return "8"
    if (test(C3(), Base()) != 6) return "9"

    return "OK"
}