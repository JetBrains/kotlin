// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
enum class Some {
    FIRST,
    SECOND;
}

fun foo(s: Some) = when (s) {
    FIRST -> <!UNRESOLVED_REFERENCE!>SECOND<!>
    SECOND -> <!UNRESOLVED_REFERENCE!>FIRST<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, smartcast, whenExpression,
whenWithSubject */
