// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun noArgs() {}
fun oneLambdaArg(fn: () -> Unit) {}
fun twoLambdaArgs(f1: () -> Unit, f2: () -> Unit) {}
fun varargFn(vararg args: Int) {}

fun testNoArgs() {
    noArgs()
    <!INAPPLICABLE_CANDIDATE!>noArgs<!> {}
    <!INAPPLICABLE_CANDIDATE!>noArgs<!>() {}
    noArgs() // {}
    <!INAPPLICABLE_CANDIDATE!>noArgs<!>() /* */ {}
    <!INAPPLICABLE_CANDIDATE!>noArgs<!>() /*
        block comment, no new line
    */ {}
    <!INAPPLICABLE_CANDIDATE!>noArgs<!>()
    /*
        block comment with new line
    */
    {}
    <!INAPPLICABLE_CANDIDATE!>noArgs<!>() // comment
    // comment
    {}
    <!INAPPLICABLE_CANDIDATE!>noArgs<!>() {} {}
    <!INAPPLICABLE_CANDIDATE!>noArgs<!>() {}
    {}
}

fun testLambdaArg() {
    <!INAPPLICABLE_CANDIDATE!>oneLambdaArg<!>()
    oneLambdaArg {}
    oneLambdaArg()
    {}
    <!INAPPLICABLE_CANDIDATE!>oneLambdaArg<!>()
    {}
    {}
    <!INAPPLICABLE_CANDIDATE!>oneLambdaArg<!>(
        {},
        {}
    )
    oneLambdaArg() {}
    <!INAPPLICABLE_CANDIDATE!>oneLambdaArg<!>() // {}
    oneLambdaArg() /* */ {}
    oneLambdaArg() /*
        block
        comment
    */ {}
    oneLambdaArg() // comment
    // comment
    {}
    <!INAPPLICABLE_CANDIDATE!>oneLambdaArg<!>() {}/*
        block comment, no new line
    */ {}
    <!INAPPLICABLE_CANDIDATE!>oneLambdaArg<!>() {}/*
        block comment with new line
    */
    {}
    <!INAPPLICABLE_CANDIDATE!>oneLambdaArg<!>() {}// comment
    // comment
    {}
    <!INAPPLICABLE_CANDIDATE!>oneLambdaArg<!>() {} {}
    <!INAPPLICABLE_CANDIDATE!>oneLambdaArg<!>() {}
    {}
    <!INAPPLICABLE_CANDIDATE!>oneLambdaArg<!>() {} // comment
    {}
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
    varargFn(1,2,3) {} <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
    varargFn(1,2,3) {}
    <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
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
            <!INAPPLICABLE_CANDIDATE!>twoLambdaArgs<!>({})
            {}
            {}
        } else {
            {}
        }
    }
}

fun f1(): (() -> Unit) -> (() -> Unit) -> Unit {
    return { l1 ->
        <!INAPPLICABLE_CANDIDATE!>l1<!>()
        { l2 -> <!UNRESOLVED_REFERENCE!>l2<!>() }
    }
}
