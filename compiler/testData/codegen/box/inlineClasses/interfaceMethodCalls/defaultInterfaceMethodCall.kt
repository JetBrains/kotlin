// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

interface IFoo {
    fun foo() = bar()
    fun bar(): String
}

inline class Z(val x: Int) : IFoo {
    override fun bar(): String = "OK"
}

inline class L(val x: Long) : IFoo {
    override fun bar(): String = "OK"
}

inline class S(val x: String) : IFoo {
    override fun bar(): String = "OK"
}

fun box(): String {
    if (Z(42).foo() != "OK") throw AssertionError()
    if (L(4L).foo() != "OK") throw AssertionError()
    if (S("").foo() != "OK") throw AssertionError()

    return "OK"
}