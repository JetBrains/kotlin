// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

interface A {
    fun f(x: String) = x
}

inline class B(val y: String) : A {
    override fun f(x: String) = super.f(x + y)
}

fun box(): String {
    return B("K").f("O")
}
