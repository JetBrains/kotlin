// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    fun foo(vararg x: T) {}
}

fun test(a: A<out CharSequence>, y: Array<out CharSequence>) {
    a.<!OI;MEMBER_PROJECTED_OUT!>foo<!>(<!NI;TYPE_MISMATCH!>""<!>, <!NI;TYPE_MISMATCH!>""<!>, <!NI;TYPE_MISMATCH!>""<!>)
    a.foo(*<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>y<!>)
    // TODO: TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS probably redundant
    a.<!OI;MEMBER_PROJECTED_OUT!>foo<!>(*<!NI;TYPE_MISMATCH, OI;TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS!>y<!>, <!NI;TYPE_MISMATCH!>""<!>)
}
