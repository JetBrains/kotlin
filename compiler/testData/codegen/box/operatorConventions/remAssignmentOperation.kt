// !LANGUAGE: -ProhibitOperatorMod
// IGNORE_BACKEND_FIR: JVM_IR

class A() {
    var x = 5
}

operator fun A.modAssign(y: Int) { throw RuntimeException("mod has been called instead of rem") }
operator fun A.remAssign(y: Int) { x %= y + 1 }

fun box(): String {
    val original = A()
    val a = original

    a %= 2
    if (a !== original) return "Fail 1: $a !== $original"
    if (a.x != 2) return "Fail 2: ${a.x} != 2"

    return "OK"
}