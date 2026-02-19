// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 INVOKEDYNAMIC typeSwitch
// 0 INSTANCEOF

open class Base
class C1 : Base()
class C2 : Base()
class C3 : Base()

fun test(k: Base?): Int {
    return when (k) {
        is C1, null, is C3 -> 1
        is C2 -> 2
        else -> 0
    }
}

fun box(): String {
    if (test(Base()) != 0) return "0"
    if (test(C1()) != 1) return "1"
    if (test(C2()) != 2) return "2"
    if (test(C3()) != 1) return "3"
    if (test(null) != 1) return "null"

    return "OK"
}