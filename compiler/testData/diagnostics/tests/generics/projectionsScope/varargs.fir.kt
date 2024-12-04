// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    fun foo(vararg x: T) {}
}

fun test(a: A<out CharSequence>, y: Array<out CharSequence>) {
    a.foo(<!MEMBER_PROJECTED_OUT!>""<!>, <!MEMBER_PROJECTED_OUT!>""<!>, <!MEMBER_PROJECTED_OUT!>""<!>)
    a.foo(*<!ARGUMENT_TYPE_MISMATCH!>y<!>)
    // TODO: TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS probably redundant
    a.foo(*<!ARGUMENT_TYPE_MISMATCH!>y<!>, <!MEMBER_PROJECTED_OUT!>""<!>)
}
