// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class A(val x: String)

class B {
    override fun equals(other: Any?) = true
}

fun box(): String {
    val x: Any? = B()
    val y: A = A("")
    if (x != y) return "Fail"
    return "OK"
}
