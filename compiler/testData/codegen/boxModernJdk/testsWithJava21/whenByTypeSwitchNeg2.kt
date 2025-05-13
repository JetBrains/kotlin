// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 0 INVOKEDYNAMIC typeSwitch

open class Base
class C1 : Base()
class C2 : Base()
class C3 : Base()

fun test(k: Base): Int {
    return when(k) {
        is C1 -> 1
        !is C3 -> 2
        is C3 -> 3
        else -> 0
    }
}

fun box(): String {
    if (test(C1()) != 1) return "1"
    if (test(C2()) != 2) return "2"
    if (test(C3()) != 3) return "3"

    return "OK"
}