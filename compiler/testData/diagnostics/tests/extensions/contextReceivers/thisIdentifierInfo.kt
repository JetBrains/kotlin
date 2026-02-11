// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

class A(val a: String?)

context(A) fun f() {
    if (this@A.a == null) return
    <!DEBUG_INFO_SMARTCAST!>this@A.a<!>.length
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, functionDeclarationWithContext,
ifExpression, nullableType, primaryConstructor, propertyDeclaration, smartcast, thisExpression */
