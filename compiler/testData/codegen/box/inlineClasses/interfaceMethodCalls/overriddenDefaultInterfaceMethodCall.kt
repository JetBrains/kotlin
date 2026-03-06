// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

interface IBase {
    fun foo() = "BAD"
}

interface IFoo : IBase {
    override fun foo() = "OK"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val x: Int) : IFoo

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(val x: Long) : IFoo

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val x: String) : IFoo

fun box(): String {
    if (Z(42).foo() != "OK") throw AssertionError()
    if (L(4L).foo() != "OK") throw AssertionError()
    if (S("").foo() != "OK") throw AssertionError()

    return "OK"
}