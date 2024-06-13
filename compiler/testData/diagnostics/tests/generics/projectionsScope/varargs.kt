// DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    fun foo(vararg x: T) {}
}

fun test(a: A<out CharSequence>, y: Array<out CharSequence>) {
    a.foo(<!TYPE_MISMATCH!>""<!>, <!TYPE_MISMATCH!>""<!>, <!TYPE_MISMATCH!>""<!>)
    a.foo(*<!TYPE_MISMATCH!>y<!>)
    // TODO: TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS probably redundant
    a.foo(*<!TYPE_MISMATCH!>y<!>, <!TYPE_MISMATCH!>""<!>)
}
