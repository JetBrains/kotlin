// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-77417

class OutVariance<out T> {
    context(a: <!TYPE_VARIANCE_CONFLICT_ERROR!>T<!>)
    fun foo(b: <!TYPE_VARIANCE_CONFLICT_ERROR!>T<!>) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, nullableType, out,
typeParameter */
