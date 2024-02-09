// ISSUE: KT-58665

fun <R, T : Any> use(x: String?, r: R, t: T) {
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!ARGUMENT_TYPE_MISMATCH!>r<!>)
    foo(t)
}

fun <W> foo(x: W & Any) {}