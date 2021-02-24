// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    fun foo(vararg x: T) {}
}

fun test(a: A<out CharSequence>, y: Array<out CharSequence>) {
    a.<!INAPPLICABLE_CANDIDATE!>foo<!>("", "", "")
    a.<!INAPPLICABLE_CANDIDATE!>foo<!>(*y)
    // TODO: TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS probably redundant
    a.<!INAPPLICABLE_CANDIDATE!>foo<!>(*y, "")
}
