// DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    fun foo(vararg x: T) {}
}

fun test(a: A<out CharSequence>, y: Array<out CharSequence>) {
    a.foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>, <!ARGUMENT_TYPE_MISMATCH!>""<!>, <!ARGUMENT_TYPE_MISMATCH!>""<!>)
    a.foo(*<!ARGUMENT_TYPE_MISMATCH!>y<!>)
    // TODO: TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS probably redundant
    a.foo(*<!ARGUMENT_TYPE_MISMATCH!>y<!>, <!ARGUMENT_TYPE_MISMATCH!>""<!>)
}
