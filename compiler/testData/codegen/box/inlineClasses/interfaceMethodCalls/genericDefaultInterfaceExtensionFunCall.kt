// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

interface IFoo<T : IFoo<T>> {
    fun T.foo(): String = bar()
    fun bar(): String
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int) : IFoo<Z> {
    override fun bar(): String = "OK"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(val x: Long) : IFoo<L> {
    override fun bar(): String = "OK"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String) : IFoo<S> {
    override fun bar(): String = x
}

fun Z.testZ() {
    if (Z(42).foo() != "OK") throw AssertionError()
}

fun L.testL() {
    if (L(4L).foo() != "OK") throw AssertionError()
}

fun S.testS() {
    if (S("OK").foo() != "OK") throw AssertionError()
}

fun box(): String {
    Z(42).testZ()
    L(4L).testL()
    S("").testS()

    return "OK"
}
