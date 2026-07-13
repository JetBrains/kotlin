// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-87238

inline fun select(a: () -> String, b: () -> Unit) = 1
fun select(a: () -> Int, b: () -> Unit) = "2"

inline fun selectWithAny(a: () -> Any, b: () -> Unit) = 1
fun selectWithAny(a: () -> Int, b: () -> Unit) = "2"

inline fun selectWithUnitOnFirst(a: () -> Unit, b: () -> Int) = println("1")
fun selectWithUnitOnFirst(a: ()-> String, b: () -> Unit) = println("2")

inline fun test(x: () -> Unit) {
    select({ 1 }, <!USAGE_IS_NOT_INLINABLE!>x<!>)
    select({ "" }, x)

    selectWithAny({ 1 }, <!USAGE_IS_NOT_INLINABLE!>x<!>)
    selectWithAny({ "" }, x)

    selectWithUnitOnFirst({ "" }, <!USAGE_IS_NOT_INLINABLE!>x<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, integerLiteral, lambdaLiteral, stringLiteral */
