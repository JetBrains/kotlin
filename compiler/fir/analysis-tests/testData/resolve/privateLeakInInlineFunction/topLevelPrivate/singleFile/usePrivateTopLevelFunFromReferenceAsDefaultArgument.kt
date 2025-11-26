// RUN_PIPELINE_TILL: FRONTEND
// NO_CHECK_LAMBDA_INLINING
private fun funOK() = "OK"

internal inline fun internalInlineFun(ok: () -> String = ::<!CALLABLE_REFERENCE_TO_LESS_VISIBLE_DECLARATION_IN_INLINE_ERROR!>funOK<!>) = ok()

fun box(): String {
    return internalInlineFun()
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, inline, stringLiteral */
