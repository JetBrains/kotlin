// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

interface IFoo {
    fun Long.foo() = bar()
    fun bar(): String
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int) : IFoo {
    override fun bar(): String = "OK"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(val x: Long) : IFoo {
    override fun bar(): String = "OK"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String) : IFoo {
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