// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

@JvmName("spreadUnit")
fun spread(vararg blocks: () -> Unit) = 1
fun spread(vararg blocks: () -> String) = "(2)"

fun testSpreadArrayArgument() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>spread<!>(*arrayOf({ "" }))
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>spread<!>(*arrayOf({ Unit }))
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>spread<!>(*arrayOf({ TODO() }))
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>spread<!>(*arrayOf({ 1 }))
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>spread<!>(*arrayOf({ "" }, { Unit }))
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, stringLiteral, vararg */
