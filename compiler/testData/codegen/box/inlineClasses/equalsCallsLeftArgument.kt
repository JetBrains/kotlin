// !LANGUAGE: +InlineClasses

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
