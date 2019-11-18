// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

inline class Z(val x: Int)
inline class L(val x: Long)
inline class S(val x: String)

fun box(): String {
    if ((Z::x).get(Z(42)) != 42) throw AssertionError()
    if ((L::x).get(L(1234L)) != 1234L) throw AssertionError()
    if ((S::x).get(S("abcdef")) != "abcdef") throw AssertionError()

    return "OK"
}