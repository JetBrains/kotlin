// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-88961

// COMPARE_WITH_LIGHT_TREE
// REASON: the light-tree checker misses prefixes/suffixes outside binary expressions

// Here the literal sits at the edge of a parent that is not a binary expression, so the adjacent leaf can only be
// reached by walking further up the tree. `FirPrefixAndSuffixSyntaxChecker.getLeaf` stops climbing at the first
// parent that is not a `BINARY_EXPRESSION`, so the light tree misses these while PSI reports them.

// Note there is no integer counterpart for the prefix cases: a keyword directly followed by a digit lexes as a single
// identifier (`in1`), so only the suffix cases in NumberPrefixAndSuffix.kt are reachable.

fun testReturn(): String {
    <!UNSUPPORTED!>return<!>"str"
}

fun testFor() {
    for (c <!UNSUPPORTED{PSI}!>in<!>"abc") {}
    for (c <!UNSUPPORTED{PSI}!>in<!>'a'..'z') {}
}

fun testElse(): String {
    return if (true) "a" <!UNSUPPORTED{PSI}!>else<!>"b"
}

fun testNoFalsePositives() {
    val viaWhitespace = if (true) "a" else "b"
    val viaParentheses = ("a")
    val viaCall = listOf("a", "b")
    val viaIndex = listOf("a")["a".length]
    val viaArrow = when (1) { else ->"a" }
    val viaLambda = run {"a"}
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, ifExpression, lambdaLiteral, localProperty, propertyDeclaration,
rangeExpression, stringLiteral, whenExpression, whenWithSubject */
