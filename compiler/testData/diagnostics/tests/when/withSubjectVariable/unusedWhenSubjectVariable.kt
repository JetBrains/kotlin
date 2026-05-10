// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_VARIABLE

fun foo(): Any = 42

fun test() {
    when (val x = foo()) {
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, propertyDeclaration, whenExpression,
whenWithSubject */
