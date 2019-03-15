// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

inline class Z(val x: Int)
inline class L(val x: Long)
inline class S(val x: String)

fun Z.test() = x
fun L.test() = x
fun S.test() = x

fun box(): String {
    if (Z(42)::test.invoke() != 42) throw AssertionError()
    if (L(1234L)::test.invoke() != 1234L) throw AssertionError()
    if (S("abcdef")::test.invoke() != "abcdef") throw AssertionError()

    return "OK"
}