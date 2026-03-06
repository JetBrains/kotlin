// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val s: T)

interface B<T> {
    fun f(x: T): T
}

open class C {
    open fun f(x: A<String>): A<String> = A("OK")
}

class D : C(), B<A<String>>

fun box(): String {
    return (D() as B<A<String>>).f(A("Fail")).s
}
