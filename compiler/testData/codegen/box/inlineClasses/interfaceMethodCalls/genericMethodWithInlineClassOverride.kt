// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val s: String)

abstract class B<T> {
    abstract fun f(x: T): T
}

class C: B<A>() {
    override fun f(x: A): A = x
}

fun box(): String {
    return C().f(A("OK")).s
}
