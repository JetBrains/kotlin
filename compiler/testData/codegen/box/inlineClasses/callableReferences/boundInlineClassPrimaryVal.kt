// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

inline class Z(val x: Int)
inline class L(val x: Long)
inline class S(val x: String)

fun box(): String {
    if (Z(42)::x.get() != 42) throw AssertionError()
    if (L(1234L)::x.get() != 1234L) throw AssertionError()
    if (S("abcdef")::x.get() != "abcdef") throw AssertionError()

    return "OK"
}