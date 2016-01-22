// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    fun foo(vararg x: T) {}
}

fun test(a: A<out CharSequence>, y: Array<out CharSequence>) {
    a.<!MEMBER_PROJECTED_OUT!>foo<!>("", "", "")
    a.foo(*<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>y<!>)
    // TODO: TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS probably redundant
    a.<!MEMBER_PROJECTED_OUT!>foo<!>(*<!TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>y<!>, "")
}
