// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// DIAGNOSTICS: -UNUSED_PARAMETER

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
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    noArgs() // comment
    // comment
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    noArgs() <!TOO_MANY_ARGUMENTS!>{}<!> <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    noArgs() <!TOO_MANY_ARGUMENTS!>{}<!>
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
}

fun testLambdaArg() {
    oneLambdaArg<!NO_VALUE_FOR_PARAMETER!>()<!>
    oneLambdaArg {}
    oneLambdaArg()
    {}
    oneLambdaArg()
    {}
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
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
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    oneLambdaArg() {}// comment
    // comment
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    oneLambdaArg() {} <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    oneLambdaArg() {}
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    oneLambdaArg() {} // comment
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
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
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
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
        <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>

        return <!RETURN_TYPE_MISMATCH!>if (true) {
            twoLambdaArgs({})
            {}
            <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
        } else {
            {}
        }<!>
    }
}

fun f1(): (() -> Unit) -> (() -> Unit) -> Unit {
    return <!RETURN_TYPE_MISMATCH!>{ l1 ->
        l1()
        <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{ <!CANNOT_INFER_PARAMETER_TYPE!>l2<!> -> <!UNRESOLVED_REFERENCE!>l2<!>() }<!>
    }<!>
}
