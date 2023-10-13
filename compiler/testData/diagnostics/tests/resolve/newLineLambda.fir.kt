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
    noArgs() <!TOO_MANY_ARGUMENTS!>{}<!> <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    noArgs() <!TOO_MANY_ARGUMENTS!>{}<!>
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
}

fun testLambdaArg() {
    oneLambdaArg<!NO_VALUE_FOR_PARAMETER!>()<!>
    oneLambdaArg {}
    oneLambdaArg()
    {}
    oneLambdaArg()
    {}
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    oneLambdaArg(
        {},
        <!TOO_MANY_ARGUMENTS!>{}<!>
    )
    oneLambdaArg() {}
    oneLambdaArg<!NO_VALUE_FOR_PARAMETER!>()<!> // {}
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
    */ <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    oneLambdaArg() {}/*
        block comment with new line
    */
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    oneLambdaArg() {}// comment
    // comment
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    oneLambdaArg() {} <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    oneLambdaArg() {}
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    oneLambdaArg() {} // comment
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
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
    varargFn(1,2,3) <!VARARG_OUTSIDE_PARENTHESES!>{}<!> <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    varargFn(1,2,3) <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
}

fun testTwoLambdas() {
    twoLambdaArgs(
        f1 = {},
        f2 =
        {}
    )

    fun bar(): () -> Unit {
        twoLambdaArgs<!NO_VALUE_FOR_PARAMETER!>()<!>
        {}
        <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>

        return <!RETURN_TYPE_MISMATCH!>if (true) {
            twoLambdaArgs({})
            {}
            <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
        } else {
            {}
        }<!>
    }
}

fun f1(): (() -> Unit) -> (() -> Unit) -> Unit {
    return <!RETURN_TYPE_MISMATCH!>{ l1 ->
        l1()
        <!TOO_MANY_ARGUMENTS!>{ l2 -> <!UNRESOLVED_REFERENCE!>l2<!>() }<!>
    }<!>
}
