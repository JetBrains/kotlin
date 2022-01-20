// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class X(val x: String): IC()

interface IFoo1<T> {
    fun foo(x: T): IC
}

interface IFoo2 {
    fun foo(x: String): IC
}

class Test : IFoo1<String>, IFoo2 {
    override fun foo(x: String): IC = X(x)
}

fun box(): String {
    val t1: IFoo1<String> = Test()
    val t2: IFoo2 = Test()
    return t1.foo("O").x + t2.foo("K").x
}