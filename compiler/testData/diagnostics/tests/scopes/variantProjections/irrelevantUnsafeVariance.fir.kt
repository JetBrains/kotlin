// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57609

interface A<T> {
    fun foo(x: @UnsafeVariance T)
}

fun foo(a1: A<out Any?>, a2: A<*>) {
    a1.foo(<!MEMBER_PROJECTED_OUT!>""<!>)
    a2.foo(<!MEMBER_PROJECTED_OUT!>""<!>)
}