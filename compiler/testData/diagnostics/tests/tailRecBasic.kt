// FIR_IDENTICAL
// WITH_STDLIB
// DIAGNOSTICS: -UNREACHABLE_CODE

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo1() {
    <!NON_TAIL_RECURSIVE_CALL!>foo1<!>()
    1
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo2() {
    <!NON_TAIL_RECURSIVE_CALL!>foo2<!>()
    val i = 1
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo3() {
    <!NON_TAIL_RECURSIVE_CALL!>foo3<!>()
    foo1()
}

tailrec fun foo4() {
    if (true) foo4()
    else foo3()
}

tailrec fun foo5() {
    return foo5()
    foo4()
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo6(b: Boolean) {
    while (b) {
        <!NON_TAIL_RECURSIVE_CALL!>foo6<!>(!b)
    }
}

tailrec fun foo7_return() {
    while (true) {
        foo7_return()
        return
    }
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo7_break() {
    while (true) {
        <!NON_TAIL_RECURSIVE_CALL!>foo7_break<!>()
        break
    }
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo7_continue() {
    while (true) {
        <!NON_TAIL_RECURSIVE_CALL!>foo7_continue<!>()
        continue
    }
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo8() {
    while (true) {
        <!NON_TAIL_RECURSIVE_CALL!>foo8<!>()
        throw Exception()
    }
    foo8()
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo9() {
    <!NON_TAIL_RECURSIVE_CALL!>foo9<!>()
    fun bar() {}
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo10() {
    <!NON_TAIL_RECURSIVE_CALL!>foo10<!>()
    class Bar {
        val i = 1
    }
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo11(): String {
    return "hello ${<!NON_TAIL_RECURSIVE_CALL!>foo11<!>()}"
}

