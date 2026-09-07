// RUN_PIPELINE_TILL: BACKEND
// ISSUES: KT-81932

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo1() {
    try {
        foo1()
    } catch (e: Exception) {
        foo1()
    } finally {
        foo1()
    }
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo2() {
    try {
        foo2()
        foo1()
    } catch (e: Exception) {
        foo2()
        foo1()
    } finally {
        foo2()
        foo1()
    }
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo3() {
    try {
        try {
            foo3()
        } finally {
        }
    } catch (e: Exception) {
        try {
            foo3()
        } finally {
        }
    } finally {
        try {
            foo3()
        } finally {
        }
    }
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo4() {
    try {
        if (true) {
            foo4()
        } else {
            foo1()
        }
    } catch (e: Exception) {
        if (true) {
            foo4()
        } else {
            foo1()
        }
    } finally {
        if (true) {
            foo4()
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

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, localProperty, propertyDeclaration, tailrec, tryExpression */
