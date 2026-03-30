// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +VariableDeclarationInWhenSubject
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun test(x: Any) {
    when (val y = x) {
        is String -> {}
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, localProperty, propertyDeclaration, whenExpression,
whenWithSubject */
