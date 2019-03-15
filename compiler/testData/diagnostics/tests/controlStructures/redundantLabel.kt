@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun testLambdaLabel() = l@ { 42 }

fun testAnonymousFunctionLabel() = l@ fun() {}

fun testAnnotatedLambdaLabel() = lambda@ @Ann {}

fun testParenthesizedLambdaLabel() = lambda@ ( {} )

fun testLabelBoundToInvokeOperatorExpression() = <!REDUNDANT_LABEL_WARNING!>l@<!> { 42 }()

fun testLabelBoundToLambda() = (l@ { 42 })()

fun testWhileLoopLabel() {
    L@ while (true) {}
}

fun testDoWhileLoopLabel() {
    L@ do {} while (true)
}

fun testForLoopLabel(xs: List<Any>) {
    L@ for (x in xs) {}
}

fun testValLabel() {
    <!REDUNDANT_LABEL_WARNING!>L@<!> val fn = {}
    fn()
}

fun testHighOrderFunctionCallLabel() {
    <!REDUNDANT_LABEL_WARNING!>L@<!> run {}
}

fun testAnonymousObjectLabel() =
    <!REDUNDANT_LABEL_WARNING!>L@<!> object {}