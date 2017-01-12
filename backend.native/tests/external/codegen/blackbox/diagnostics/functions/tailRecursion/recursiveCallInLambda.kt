// !DIAGNOSTICS: -UNUSED_PARAMETER

// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND_WITHOUT_CHECK: JS

<!NO_TAIL_CALLS_FOUND!>tailrec fun foo()<!> {
    bar {
        <!NON_TAIL_RECURSIVE_CALL!>foo<!>()
    }
}

fun bar(a: Any) {}

fun box(): String {
    foo()
    return "OK"
}