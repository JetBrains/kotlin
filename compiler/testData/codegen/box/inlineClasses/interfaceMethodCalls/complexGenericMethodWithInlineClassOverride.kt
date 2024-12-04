// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val s: String)

abstract class B<T, U> {
    abstract fun f(x: T, y: U): String
}

open class C<T>: B<T, A>() {
    override fun f(x: T, y: A): String = y.s + " 1"
}

open class D : C<A>() {
    override fun f(x: A, y: A): String = y.s + " 2"
}

class E : D() {
    override fun f(x: A, y: A): String = x.s
}

fun box(): String {
    return E().f(A("OK"), A("Fail"))
}
