// !LANGUAGE: +InlineClasses

inline class A(val i: Int) {
    fun foo(s: String = "OK") = s
}

fun box() = A(42).foo()