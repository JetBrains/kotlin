<!NO_TAIL_CALLS_FOUND!>tailRecursive fun foo()<!> {
    fun bar() {
        <!NON_TAIL_RECURSIVE_CALL!>foo<!>()
    }
}