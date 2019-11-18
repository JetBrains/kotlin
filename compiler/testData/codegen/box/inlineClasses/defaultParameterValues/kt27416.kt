// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class A(val i: Int) {
    fun foo(s: String = "OK") = s
}

fun box() = A(42).foo()