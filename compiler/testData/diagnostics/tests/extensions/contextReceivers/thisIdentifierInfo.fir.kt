// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

class A(val a: String?)

context(A) fun f() {
    if (this@A.a == null) return
    this@A.a.length
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, functionDeclarationWithContext,
ifExpression, nullableType, primaryConstructor, propertyDeclaration, smartcast, thisExpression */
