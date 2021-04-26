// !WITH_NEW_INFERENCE
// WITH_RUNTIME

fun Runnable.test(f: Runnable.(Int) -> Unit) {
    f(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
}

fun test(f: Runnable.(Int) -> Unit, runnable: Runnable) {
    with (runnable) {
        f(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    }
}

fun Int.test(f: String.(Int) -> Unit) {
    f("", 0)
    <!ARGUMENT_TYPE_MISMATCH!>f<!>(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
    with("") {
        f(0)
        f(<!ARGUMENT_TYPE_MISMATCH!>0.0<!>)
    }
}