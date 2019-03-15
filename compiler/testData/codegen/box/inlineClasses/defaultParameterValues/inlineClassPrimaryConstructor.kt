// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Z(val x: Int = 1234)
inline class L(val x: Long = 1234L)
inline class S(val x: String = "foobar")

fun box(): String {
    if (Z().x != 1234) throw AssertionError()
    if (L().x != 1234L) throw AssertionError()
    if (S().x != "foobar") throw AssertionError()

    return "OK"
}