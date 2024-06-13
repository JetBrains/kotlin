// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun <T : CharSequence?> bar1(x: T) {}

fun bar2(x: CharSequence?) {}

fun <T : CharSequence> bar3(x: T) {}

fun bar4(x: String) {}

fun <T : String?> foo(x: T) {
    bar1(x)
    bar2(x)

    <!CANNOT_INFER_PARAMETER_TYPE!>bar3<!>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    bar4(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}
