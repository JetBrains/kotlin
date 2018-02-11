// !WITH_NEW_INFERENCE

interface A<out K> {
    fun foo(x: @UnsafeVariance K): Unit
}

fun test(a: A<*>) {
    a.foo(<!NI;NULL_FOR_NONNULL_TYPE!>null<!>)
    a.foo(<!NI;TYPE_MISMATCH!>Any()<!>)
}
