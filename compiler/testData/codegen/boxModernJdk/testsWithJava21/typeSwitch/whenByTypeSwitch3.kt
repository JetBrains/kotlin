// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 INVOKEDYNAMIC typeSwitch
// 0 INSTANCEOF

open class Base {
    open fun some() = this
}
class C1 : Base() {
    override fun some() = C1()
}
class C2 : Base()
class C3 : Base()

fun test(k: Base): Int {
    return when(k.some()) {
        is C1 -> 1
        is C2 -> 2
        else -> 0
    }
}

fun box(): String {
    if (test(Base()) != 0) return "0"
    if (test(C1()) != 1) return "1"
    if (test(C2()) != 2) return "2"
    if (test(C3()) != 0) return "3"

    return "OK"
}