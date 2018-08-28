// !DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_BACKEND: JS_IR

// TODO: muted automatically, investigate should it be ran for JS or not
// DONT_RUN_GENERATED_CODE: JS
// IGNORE_BACKEND: JS

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