// !LANGUAGE: -RestrictReturnStatementTarget

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun testFunctionName() {
    return@testFunctionName
}

fun testHighOrderFunctionName() {
    run {
        return@run
    }
}

fun testLambdaLabel() =
    lambda@ {
        return@lambda
    }

fun testParenthesizedLambdaLabel() =
    lambda@ ( {
        return@lambda
    } )

fun testAnnotatedLambdaLabel() =
    lambda@ @Ann {
        return@lambda
    }

fun testLambdaMultipleLabels1() =
    <!MULTIPLE_LABELS_ARE_FORBIDDEN!>lambda1@<!> lambda2@ {
        <!NOT_A_FUNCTION_LABEL!>return@lambda1<!>
    }

fun testLambdaMultipleLabels2() =
    <!MULTIPLE_LABELS_ARE_FORBIDDEN!>lambda1@<!> lambda2@ {
        return@lambda2
    }

fun testAnonymousFunctionLabel() =
    anonFun@ fun() {
        return@anonFun
    }

fun testLoopLabelInReturn(xs: List<Int>) {
    L@ for (x in xs) {
        if (x > 0) <!NOT_A_FUNCTION_LABEL!>return@L<!>
    }
}

fun testValLabelInReturn() {
    L@ val fn = { <!NOT_A_FUNCTION_LABEL!>return@L<!> }
    fn()
}

fun testHighOrderFunctionCallLabelInReturn() {
    L@ run {
        <!NOT_A_FUNCTION_LABEL!>return@L<!>
    }
}

fun testMultipleLabelsWithNestedLambda() {
    <!MULTIPLE_LABELS_ARE_FORBIDDEN!>l1@<!> l2@{
        {
            <!NOT_A_FUNCTION_LABEL!>return@l1<!>
        }
        return@l2
    }
}
