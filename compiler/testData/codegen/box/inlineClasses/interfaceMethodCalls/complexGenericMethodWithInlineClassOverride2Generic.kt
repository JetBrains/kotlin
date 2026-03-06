// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val s: T)

interface B<T, U> {
    fun f(x: T, y: U): String
}

interface L<T> {
    fun f(x: T, y: A<String>): String
}

interface R<T> {
    fun f(x: A<String>, y: T): String
}

open class C {
    open fun f(x: A<String>, y: A<String>): String = y.s
}

class D: C(), B<A<String>, A<String>>, L<A<String>>, R<A<String>> {
    override fun f(x: A<String>, y: A<String>): String = x.s
}

fun box(): String {
    return (D() as B<A<String>, A<String>>).f(A("OK"), A("Fail"))
}
