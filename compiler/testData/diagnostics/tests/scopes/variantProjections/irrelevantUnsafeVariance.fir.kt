// ISSUE: KT-57609

interface A<T> {
    fun foo(x: @UnsafeVariance T)
}

fun foo(a1: A<out Any?>, a2: A<*>) {
    a1.foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    a2.foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
}