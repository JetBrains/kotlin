// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface IBase {
    fun foo() = "BAD"
}

interface IFoo : IBase {
    override fun foo() = "OK"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T) : IFoo

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(val x: T) : IFoo

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val x: T) : IFoo

fun box(): String {
    if (Z(42).foo() != "OK") throw AssertionError()
    if (L(4L).foo() != "OK") throw AssertionError()
    if (S("").foo() != "OK") throw AssertionError()

    return "OK"
}