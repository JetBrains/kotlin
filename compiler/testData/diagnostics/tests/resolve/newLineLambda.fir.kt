// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun noArgs() {}
fun oneLambdaArg(fn: () -> Unit) {}
fun twoLambdaArgs(f1: () -> Unit, f2: () -> Unit) {}
fun varargFn(vararg args: Int) {}

fun testNoArgs() {
    noArgs()
    noArgs <!TOO_MANY_ARGUMENTS!>{}<!>
    noArgs() <!TOO_MANY_ARGUMENTS!>{}<!>
    noArgs() // {}
    noArgs() /* */ <!TOO_MANY_ARGUMENTS!>{}<!>
    noArgs() /*
        block comment, no new line
    */ <!TOO_MANY_ARGUMENTS!>{}<!>
    noArgs()
    /*
        block comment with new line
    */
    <!TOO_MANY_ARGUMENTS!>{}<!>
    noArgs() // comment
    // comment
    <!TOO_MANY_ARGUMENTS!>{}<!>
    noArgs() <!TOO_MANY_ARGUMENTS!>{}<!> <!TOO_MANY_ARGUMENTS!>{}<!>
    noArgs() <!TOO_MANY_ARGUMENTS!>{}<!>
    <!TOO_MANY_ARGUMENTS!>{}<!>
}

fun testLambdaArg() {
    oneLambdaArg(<!NO_VALUE_FOR_PARAMETER!>)<!>
    oneLambdaArg {}
    oneLambdaArg()
    {}
    oneLambdaArg()
    {}
    <!TOO_MANY_ARGUMENTS!>{}<!>
    oneLambdaArg(
        {},
        <!TOO_MANY_ARGUMENTS!>{}<!>
    )
    oneLambdaArg() {}
    oneLambdaArg(<!NO_VALUE_FOR_PARAMETER!>)<!> // {}
    oneLambdaArg() /* */ {}
    oneLambdaArg() /*
        block
        comment
    */ {}
    oneLambdaArg() // comment
    // comment
    {}
    oneLambdaArg() {}/*
        block comment, no new line
    */ <!TOO_MANY_ARGUMENTS!>{}<!>
    oneLambdaArg() {}/*
        block comment with new line
    */
    <!TOO_MANY_ARGUMENTS!>{}<!>
    oneLambdaArg() {}// comment
    // comment
    <!TOO_MANY_ARGUMENTS!>{}<!>
    oneLambdaArg() {} <!TOO_MANY_ARGUMENTS!>{}<!>
    oneLambdaArg() {}
    <!TOO_MANY_ARGUMENTS!>{}<!>
    oneLambdaArg() {} // comment
    <!TOO_MANY_ARGUMENTS!>{}<!>
}

fun testVararg() {
    varargFn(1,2,3)
    varargFn <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
    varargFn(1,2,3) <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
    varargFn(1,2,3) // {}
    varargFn(1,2,3) /* */ <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
    varargFn(1,2,3) /*
        block comment, no new line
    */ <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
    varargFn(1,2,3)
    /*
        block comment with new line
    */ <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
    varargFn(1,2,3) // comment
    // comment
    <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
    varargFn(1,2,3) <!ARGUMENT_TYPE_MISMATCH!>{}<!> <!ARGUMENT_TYPE_MISMATCH, VARARG_OUTSIDE_PARENTHESES!>{}<!>
    varargFn(1,2,3) <!ARGUMENT_TYPE_MISMATCH!>{}<!>
    <!ARGUMENT_TYPE_MISMATCH, VARARG_OUTSIDE_PARENTHESES!>{}<!>
}

fun testTwoLambdas() {
    twoLambdaArgs(
        f1 = {},
        f2 =
        {}
    )

    fun bar(): () -> Unit {
        twoLambdaArgs()
        {}
        {}

        return if (true) {
            twoLambdaArgs({})
            {}
            <!TOO_MANY_ARGUMENTS!>{}<!>
        } else {
            {}
        }
    }
}

fun f1(): (() -> Unit) -> (() -> Unit) -> Unit {
    return { l1 ->
        l1()
        <!TOO_MANY_ARGUMENTS!>{ l2 -> <!UNRESOLVED_REFERENCE!>l2<!>() }<!>
    }
}
