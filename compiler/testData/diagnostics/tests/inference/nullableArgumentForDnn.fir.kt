// ISSUE: KT-58665

fun <R, T : Any> use(x: String?, r: R, t: T) {
    foo(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    foo(<!ARGUMENT_TYPE_MISMATCH!>r<!>)
    foo(t)
}

fun <W> foo(x: W & Any) {}