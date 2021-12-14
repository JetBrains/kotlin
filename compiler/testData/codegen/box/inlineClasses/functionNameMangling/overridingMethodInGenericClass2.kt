// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

abstract class GenericBase<T> {
    abstract fun foo(x: T): T
}

interface IFoo {
    fun foo(x: String): String
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Str(val str: String)

class Derived : GenericBase<Str>(), IFoo {
    override fun foo(x: Str): Str = x
    override fun foo(x: String): String = x
}

fun box(): String {
    if (Derived().foo(Str("OK")).str != "OK") throw AssertionError()
    if (Derived().foo("OK") != "OK") throw AssertionError()

    return "OK"
}
