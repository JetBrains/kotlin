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
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    noArgs() // comment
    // comment
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    noArgs() <!TOO_MANY_ARGUMENTS!>{}<!> <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    noArgs() <!TOO_MANY_ARGUMENTS!>{}<!>
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS, UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
}

fun testLambdaArg() {
    oneLambdaArg(<!NO_VALUE_FOR_PARAMETER!>)<!>
    oneLambdaArg {}
    oneLambdaArg()
    {}
    oneLambdaArg()
    {}
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS, UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
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
    */ <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    oneLambdaArg() {}/*
        block comment with new line
    */
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS, UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    oneLambdaArg() {}// comment
    // comment
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS, UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    oneLambdaArg() {} <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    oneLambdaArg() {}
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS, UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    oneLambdaArg() {} // comment
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS, UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
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
    */ <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    varargFn(1,2,3) // comment
    // comment
    <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
    varargFn(1,2,3) <!VARARG_OUTSIDE_PARENTHESES!>{}<!> <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
    varargFn(1,2,3) <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
    <!MANY_LAMBDA_EXPRESSION_ARGUMENTS, UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>
}

fun testTwoLambdas() {
    twoLambdaArgs(
        f1 = {},
        f2 =
        {}
    )

    fun bar(): () -> Unit {
        twoLambdaArgs(<!NO_VALUE_FOR_PARAMETER!>)<!>
        {}
        <!MANY_LAMBDA_EXPRESSION_ARGUMENTS, UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!>

        return <!NI;TYPE_MISMATCH!>if (true) {
            <!OI;TYPE_MISMATCH!>twoLambdaArgs({})
            {}
            <!MANY_LAMBDA_EXPRESSION_ARGUMENTS, UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{}<!><!>
        } else {
            {}
        }<!>
    }
}

fun f1(): (() -> Unit) -> (() -> Unit) -> Unit {
    return { l1 ->
        <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>l1()
        <!UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE!>{ <!OI;CANNOT_INFER_PARAMETER_TYPE!>l2<!> -> <!OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!NI;FUNCTION_EXPECTED!>l2<!>()<!> }<!><!>
    }
}
