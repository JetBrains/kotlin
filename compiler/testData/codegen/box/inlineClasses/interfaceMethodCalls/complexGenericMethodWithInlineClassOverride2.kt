// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val s: String)

interface B<T, U> {
    fun f(x: T, y: U): String
}

interface L<T> {
    fun f(x: T, y: A): String
}

interface R<T> {
    fun f(x: A, y: T): String
}

open class C {
    open fun f(x: A, y: A): String = y.s
}

class D: C(), B<A, A>, L<A>, R<A> {
    override fun f(x: A, y: A): String = x.s
}

fun box(): String {
    return (D() as B<A, A>).f(A("OK"), A("Fail"))
}
