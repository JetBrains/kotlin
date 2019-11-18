// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

interface IBase {
    fun foo() = "BAD"
}

interface IFoo : IBase {
    override fun foo() = "OK"
}

inline class Z(val x: Int) : IFoo

inline class L(val x: Long) : IFoo

inline class S(val x: String) : IFoo

fun box(): String {
    if (Z(42).foo() != "OK") throw AssertionError()
    if (L(4L).foo() != "OK") throw AssertionError()
    if (S("").foo() != "OK") throw AssertionError()

    return "OK"
}