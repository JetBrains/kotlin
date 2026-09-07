// RUN_PIPELINE_TILL: BACKEND
// ISSUES: KT-81932

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo1() {
    try {
        <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo1<!>()
    } catch (e: Exception) {
        <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo1<!>()
    } finally {
        <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo1<!>()
    }
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo2() {
    try {
        <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo2<!>()
        foo1()
    } catch (e: Exception) {
        <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo2<!>()
        foo1()
    } finally {
        <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo2<!>()
        foo1()
    }
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo3() {
    try {
        try {
            <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo3<!>()
        } finally {
        }
    } catch (e: Exception) {
        try {
            <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo3<!>()
        } finally {
        }
    } finally {
        try {
            <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo3<!>()
        } finally {
        }
    }
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo4() {
    try {
        if (true) {
            <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo4<!>()
        } else {
            foo1()
        }
    } catch (e: Exception) {
        if (true) {
            <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo4<!>()
        } else {
            foo1()
        }
    } finally {
        if (true) {
            <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>foo4<!>()
        } else {
            foo1()
        }
    }
}

tailrec fun foo5(param: Int) {
    if (true) {
        return foo5(param)
    } else {
        try { } finally { }
    }
}

open class DefaultArgumentBase {
    open fun recurse(value: Int = 0) {}
}

class DefaultArgumentOverride : DefaultArgumentBase() {
    <!NO_TAIL_CALLS_FOUND!>tailrec<!> override fun recurse(value: Int) {
        try {
            <!NON_TAIL_RECURSIVE_CALL!>recurse<!>()
        } finally {
        }
    }
}

class ForeignReceiver {
    <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun recurse(other: ForeignReceiver) {
        try {
            other.<!NON_TAIL_RECURSIVE_CALL!>recurse<!>(other)
        } finally {
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, integerLiteral, localProperty, override,
propertyDeclaration, tailrec, tryExpression */
