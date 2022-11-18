// !DIAGNOSTICS: -UNUSED_PARAMETER

fun twoLambdaArgs(f1: () -> Unit, f2: () -> Unit) {}

fun testTwoLambdas() {
    twoLambdaArgs(
        f1 = {},
        f2 =
        {}
    )

    fun bar(): () -> Unit {
        <!NO_VALUE_FOR_PARAMETER!>twoLambdaArgs()<!>
        {}
        <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>

        return if (true) {
            twoLambdaArgs({})
            {}
            <!MANY_LAMBDA_EXPRESSION_ARGUMENTS!>{}<!>
        } else {
            <!ARGUMENT_TYPE_MISMATCH!>{}<!>
        }
    }
}
