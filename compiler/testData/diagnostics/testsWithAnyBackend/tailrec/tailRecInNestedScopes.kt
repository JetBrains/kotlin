// DONT_TARGET_EXACT_BACKEND: JVM_IR
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo1() {
    run {
        <!NON_TAIL_RECURSIVE_CALL!>foo1<!>()
    }
}<!>

fun myRun(f: () -> Unit) = f()


<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo2() {
    myRun {
        <!NON_TAIL_RECURSIVE_CALL!>foo2<!>()
    }
}<!>

<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo3() {
    fun bar() {
        <!NON_TAIL_RECURSIVE_CALL!>foo3<!>()
    }
    bar()
}<!>

class A {
    <!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo4() {
        with(this) {
            <!NON_TAIL_RECURSIVE_CALL!>foo4<!>()
        }
    }<!>
}

tailrec fun foo5() {
    run {
        return foo5()
    }
}

// Non-local return with `let`
tailrec fun nonLocalReturnLet(x: Int): Int {
    x.let { return nonLocalReturnLet(it - 1) }
}

// Non-local return with `also`
tailrec fun nonLocalReturnAlso(x: Int): Int {
    x.also { return nonLocalReturnAlso(it - 1) }
}

// Non-local return with `apply`
tailrec fun nonLocalReturnApply(x: Int): Int {
    x.apply { return nonLocalReturnApply(this - 1) }
}

// Nested `run` blocks with non-local return
tailrec fun nestedRunReturn(x: Int): Int {
    run {
        run {
            return nestedRunReturn(x - 1)
        }
    }
}

// Nested `run` blocks without non-local return — not a tail call
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun nestedRunNoReturn(x: Int): Int {
    run {
        run {
            <!NON_TAIL_RECURSIVE_CALL!>nestedRunNoReturn<!>(x - 1)
        }
    }
    return 0
}<!>

// `with` on a different receiver — call inside is not a tail call
class B {
    <!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo6() {
        with(B()) {
            <!NON_TAIL_RECURSIVE_CALL!>foo6<!>()
        }
    }<!>
}

// `let` without return — not a tail call
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun letNoReturn(x: Int): Int {
    x.let {
        <!NON_TAIL_RECURSIVE_CALL!>letNoReturn<!>(it - 1)
    }
    return 0
}<!>

// `also` without return — not a tail call
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun alsoNoReturn(x: Int): Int {
    x.also {
        <!NON_TAIL_RECURSIVE_CALL!>alsoNoReturn<!>(it - 1)
    }
    return 0
}<!>

// Inline lambda with conditional non-local return
tailrec fun conditionalNonLocalReturn(x: Int): Int {
    run {
        if (x > 0) return conditionalNonLocalReturn(x - 1)
    }
    return 0
}

// Multiple inline lambdas, only one has a non-local return tail call
tailrec fun multipleInlineLambdas(x: Int): Int {
    run { <!NON_TAIL_RECURSIVE_CALL!>multipleInlineLambdas<!>(x) }
    run { return multipleInlineLambdas(x - 1) }
}

// `forEach` with non-local return
tailrec fun forEachReturn(x: Int): Int {
    listOf(x).forEach { return forEachReturn(it - 1) }
    return 0
}

// Nested local class with call inside — not a tail call
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun localClassCall(x: Int): Int {
    class Local {
        fun call() = <!NON_TAIL_RECURSIVE_CALL!>localClassCall<!>(x - 1)
    }
    return Local().call()
}<!>

// Anonymous object with call inside — not a tail call
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun anonymousObjectCall(x: Int): Int {
    val obj = object {
        fun call() = <!NON_TAIL_RECURSIVE_CALL!>anonymousObjectCall<!>(x - 1)
    }
    return obj.call()
}<!>

// Lambda stored in variable, then invoked — not inline, so not a tail call
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun lambdaVarCall(x: Int): Int {
    val f = { <!NON_TAIL_RECURSIVE_CALL!>lambdaVarCall<!>(x - 1) }
    f()
    return 0
}<!>

// `if` inside `run` with non-local return in both branches
tailrec fun ifInRunBothBranches(x: Int): Int {
    run {
        if (x > 0)
            return ifInRunBothBranches(x - 1)
        else
            return ifInRunBothBranches(0)
    }
}

// `when` inside `run` with non-local return
tailrec fun whenInRun(x: Int): Int {
    run {
        when {
            x > 10 -> return whenInRun(x - 2)
            x > 0 -> return whenInRun(x - 1)
            else -> return whenInRun(0)
        }
    }
}

// Crossinline lambda — cannot have non-local return, so call is not a tail call
inline fun myCrossinlineRun(crossinline f: () -> Unit) = f()

<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun crossinlineCall(x: Int): Int {
    myCrossinlineRun {
        <!NON_TAIL_RECURSIVE_CALL!>crossinlineCall<!>(x - 1)
    }
    return 0
}<!>

// Noinline lambda — cannot have non-local return, so call is not a tail call
<!NOTHING_TO_INLINE!>inline<!> fun myNoinlineRun(noinline f: () -> Unit) = f()

<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun noinlineCall(x: Int): Int {
    myNoinlineRun {
        <!NON_TAIL_RECURSIVE_CALL!>noinlineCall<!>(x - 1)
    }
    return 0
}<!>

// Tail call inside try-catch — not supported
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun tailCallInTry(x: Int = 0): Int {
    try {
        return <!TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED!>tailCallInTry<!>(x - 1)
    } catch (e: Exception) {
        return 0
    }
}<!>

/* GENERATED_FIR_TAGS: additiveExpression, anonymousObjectExpression, classDeclaration, comparisonExpression,
crossinline, functionDeclaration, functionalType, ifExpression, integerLiteral, lambdaLiteral, localClass,
localFunction, localProperty, noinline, propertyDeclaration, tailrec, thisExpression, tryCatch, whenExpression */
