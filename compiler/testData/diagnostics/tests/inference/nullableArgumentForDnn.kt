// ISSUE: KT-58665

fun <R, T : Any> use(x: String?, r: R, t: T) {
    foo(<!TYPE_MISMATCH!>x<!>)
    foo(<!TYPE_MISMATCH!>r<!>)
    foo(t)
}

fun <W> foo(x: W & Any) {}