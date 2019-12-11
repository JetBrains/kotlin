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
        if (x > 0) return@L
    }
}

fun testValLabelInReturn() {
    L@ val fn = { return@L }
    fn()
}

fun testHighOrderFunctionCallLabelInReturn() {
    L@ run {
        return@L
    }
}