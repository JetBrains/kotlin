// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 INVOKEDYNAMIC typeSwitch
// 0 INSTANCEOF

sealed interface Base
class C1 : Base
class C2 : Base
class C3 : Base

fun test(k: Base): Int {
    return when (k) {
        is C1 -> 1
        is C2 -> 2
        is C3 -> 3
    }
}

fun box(): String {
    if (test(C1()) != 1) return "1"
    if (test(C2()) != 2) return "2"
    if (test(C3()) != 3) return "3"

    return "OK"
}