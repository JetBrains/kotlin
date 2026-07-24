// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-87151

val (suspend () -> String).bar: String
    get() = "SuspendString"

val (() -> Unit).bar: String
    get() = "Unit"

suspend fun suspendingString(): String = "OK"

fun test() {
    ({ "OK" }).bar

    ({ <!ILLEGAL_SUSPEND_FUNCTION_CALL!>suspendingString<!>() }).bar
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, getter, lambdaLiteral, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, suspend */
