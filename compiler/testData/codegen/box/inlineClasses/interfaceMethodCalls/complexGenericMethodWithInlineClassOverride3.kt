// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
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
