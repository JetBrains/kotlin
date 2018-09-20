// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

interface IFoo {
    fun Long.foo() = bar()
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

fun Z.testZ() {
    if (1L.foo() != "OK") throw AssertionError()
}

fun L.testL() {
    if (1L.foo() != "OK") throw AssertionError()
}

fun S.testS() {
    if (1L.foo() != "OK") throw AssertionError()
}

fun box(): String {
    Z(42).testZ()
    L(4L).testL()
    S("").testS()

    return "OK"
}