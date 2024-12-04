// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class X<T: String>(val x: T)

interface IFoo1<T> {
    fun foo(x: T): X<String>
}

interface IFoo2 {
    fun foo(x: String): X<String>
}

class Test : IFoo1<String>, IFoo2 {
    override fun foo(x: String): X<String> = X(x)
}

fun box(): String {
    val t1: IFoo1<String> = Test()
    val t2: IFoo2 = Test()
    return t1.foo("O").x + t2.foo("K").x
}