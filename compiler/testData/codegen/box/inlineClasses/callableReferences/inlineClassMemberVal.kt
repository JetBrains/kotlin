// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

inline class Z(val x: Int) {
    val xx get() = x
}

inline class L(val x: Long) {
    val xx get() = x
}

inline class S(val x: String) {
    val xx get() = x
}

fun box(): String {
    if ((Z::xx).get(Z(42)) != 42) throw AssertionError()
    if ((L::xx).get(L(1234L)) != 1234L) throw AssertionError()
    if ((S::xx).get(S("abcdef")) != "abcdef") throw AssertionError()

    return "OK"
}