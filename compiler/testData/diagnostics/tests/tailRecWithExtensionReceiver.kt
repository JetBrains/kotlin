// FIR_IDENTICAL
tailrec fun String.foo1() {
    "".foo1()
}

tailrec fun String.foo2() {
    this.foo2()
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun String.foo3() {
    with(this) {
        <!NON_TAIL_RECURSIVE_CALL!>foo3<!>()
    }
}
