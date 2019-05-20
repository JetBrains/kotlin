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
    lambda1@ lambda2@ {
        return@lambda1
    }

fun testLambdaMultipleLabels2() =
    lambda1@ lambda2@ {
        return@lambda2
    }

fun testAnonymousFunctionLabel() =
    anonFun@ fun() {
        return@anonFun
    }

fun testLoopLabelInReturn(xs: List<Int>) {
    L@ for (x in xs) {
        if (x > 0) <!NOT_A_FUNCTION_LABEL_WARNING!>return@L<!>
    }
}

fun testValLabelInReturn() {
    <!REDUNDANT_LABEL_WARNING!>L@<!> val fn = { <!NOT_A_FUNCTION_LABEL_WARNING!>return@L<!> }
    <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>fn<!>()
}

fun testHighOrderFunctionCallLabelInReturn() {
    <!REDUNDANT_LABEL_WARNING!>L@<!> run {
        <!NOT_A_FUNCTION_LABEL_WARNING!>return@L<!>
    }
}