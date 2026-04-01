// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76142
// RENDER_DIAGNOSTIC_ARGUMENTS

@RequiresOptIn(
    message = "This is an experimental API"
)
annotation class Experimental

@RequiresOptIn(
    message = "This is an experimental " + "API"
)
annotation class ExperimentalWithConcatenation

@Experimental
fun someFunction() {}

@ExperimentalWithConcatenation
fun anotherFunction() {}

fun test() {
    <!OPT_IN_USAGE_ERROR!>someFunction<!>()
    <!OPT_IN_USAGE_ERROR!>anotherFunction<!>()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, stringLiteral */
