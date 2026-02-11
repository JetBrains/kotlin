// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

class Context

context(Context)
fun f(): String = TODO()

fun f(): Any = TODO()

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>with<!>(Context()) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>().<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, lambdaLiteral */
