// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

interface IFoo {
    fun foo() = bar()
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

fun box(): String {
    if (Z(42).foo() != "OK") throw AssertionError()
    if (L(4L).foo() != "OK") throw AssertionError()
    if (S("").foo() != "OK") throw AssertionError()

    return "OK"
}