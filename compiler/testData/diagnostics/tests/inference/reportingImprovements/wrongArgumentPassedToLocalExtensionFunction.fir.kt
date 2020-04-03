// !WITH_NEW_INFERENCE
// WITH_RUNTIME

fun Runnable.test(f: Runnable.(Int) -> Unit) {
    <!INAPPLICABLE_CANDIDATE!>f<!>("")
}

fun test(f: Runnable.(Int) -> Unit, runnable: Runnable) {
    with (runnable) {
        <!INAPPLICABLE_CANDIDATE!>f<!>("")
    }
}

fun Int.test(f: String.(Int) -> Unit) {
    f("", 0)
    <!INAPPLICABLE_CANDIDATE!>f<!>("")
    with("") {
        f(0)
        <!INAPPLICABLE_CANDIDATE!>f<!>(0.0)
    }
}