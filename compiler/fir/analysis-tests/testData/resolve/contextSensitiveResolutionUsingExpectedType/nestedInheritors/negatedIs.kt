// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// ISSUE: KT-75977

sealed interface Simple {
    class Left: Simple
    class Right: Simple
}

fun testWithSubject(s: Simple) = when(s) {
    !is Left -> "not a left"
    <!USELESS_IS_CHECK!>!is Right<!> -> "not a right"
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, isExpression, nestedClass, sealed,
smartcast, stringLiteral, whenExpression, whenWithSubject */
