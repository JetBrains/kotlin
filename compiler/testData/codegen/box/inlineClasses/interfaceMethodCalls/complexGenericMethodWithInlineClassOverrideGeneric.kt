// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val s: T)

abstract class B<T, U> {
    abstract fun f(x: T, y: U): String
}

open class C<T>: B<T, A<String>>() {
    override fun f(x: T, y: A<String>): String = y.s + " 1"
}

open class D : C<A<String>>() {
    override fun f(x: A<String>, y: A<String>): String = y.s + " 2"
}

class E : D() {
    override fun f(x: A<String>, y: A<String>): String = x.s
}

fun box(): String {
    return E().f(A("OK"), A("Fail"))
}
