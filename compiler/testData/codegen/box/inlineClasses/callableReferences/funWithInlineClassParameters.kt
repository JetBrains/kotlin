// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

inline class Z(val x: Int)
inline class L(val x: Long)
inline class S(val x: String)

fun test(aZ: Z, aL: L, aS: S) = "${aZ.x} ${aL.x} ${aS.x}"

fun box(): String {
    if (::test.invoke(Z(1), L(1L), S("abc")) != "1 1 abc") throw AssertionError()

    return "OK"
}