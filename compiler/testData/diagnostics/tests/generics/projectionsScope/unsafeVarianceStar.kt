// FIR_IDENTICAL

interface A<out K> {
    fun foo(x: @UnsafeVariance K): Unit
}

fun test(a: A<*>) {
    a.foo(null)
    a.foo(Any())
}
