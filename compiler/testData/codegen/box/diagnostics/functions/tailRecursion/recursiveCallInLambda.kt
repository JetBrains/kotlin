// !DIAGNOSTICS: -UNUSED_PARAMETER

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo() {
    bar {
        <!NON_TAIL_RECURSIVE_CALL!>foo<!>()
    }
}

fun bar(a: Any) {}

fun box(): String {
    foo()
    return "OK"
}
