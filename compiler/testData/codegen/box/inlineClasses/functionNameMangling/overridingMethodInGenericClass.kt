// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

abstract class GenericBase<T> {
    abstract fun foo(x: T): T
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Str(val str: String)

class Derived : GenericBase<Str>() {
    override fun foo(x: Str): Str = x
}

fun box() = Derived().foo(Str("OK")).str