// FIR_IDENTICAL
// WITH_STDLIB

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo1() {
    run {
        <!NON_TAIL_RECURSIVE_CALL!>foo1<!>()
    }
}

fun myRun(f: () -> Unit) = f()


<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo2() {
    myRun {
        <!NON_TAIL_RECURSIVE_CALL!>foo2<!>()
    }
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo3() {
    fun bar() {
        <!NON_TAIL_RECURSIVE_CALL!>foo3<!>()
    }
    bar()
}

class A {
    <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo4() {
        with(this) {
            <!NON_TAIL_RECURSIVE_CALL!>foo4<!>()
        }
    }
}

tailrec fun foo5() {
    run {
        return foo5()
    }
}
