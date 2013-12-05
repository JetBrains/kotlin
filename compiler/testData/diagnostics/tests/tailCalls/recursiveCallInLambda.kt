// !DIAGNOSTICS: -UNUSED_PARAMETER
<!NO_TAIL_CALLS_FOUND!>tailRecursive fun foo()<!> {
    run {
        <!NON_TAIL_RECURSIVE_CALL!>foo<!>()
    }
}

fun run(a: Any) {}