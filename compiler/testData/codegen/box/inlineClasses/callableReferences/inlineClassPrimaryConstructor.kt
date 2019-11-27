// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

inline class Z(val x: Int)
inline class L(val x: Long)
inline class S(val x: String)

fun box(): String {
    if (42.let(::Z).x != 42) throw AssertionError()
    if (1234L.let(::L).x != 1234L) throw AssertionError()
    if ("abcdef".let(::S).x != "abcdef") throw AssertionError()

    return "OK"
}