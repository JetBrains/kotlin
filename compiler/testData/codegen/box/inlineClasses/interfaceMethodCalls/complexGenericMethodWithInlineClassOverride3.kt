// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val s: String)

interface B<T> {
    fun f(x: T): T
}

open class C {
    open fun f(x: A): A = A("OK")
}

class D : C(), B<A>

fun box(): String {
    return (D() as B<A>).f(A("Fail")).s
}
