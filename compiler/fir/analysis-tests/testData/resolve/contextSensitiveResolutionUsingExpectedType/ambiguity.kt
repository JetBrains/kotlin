// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed class Sealed {
    data object A : Sealed()
    data class B(val x: Int) : Sealed()
    data object C : Sealed()
    data class D(val y: Int) : Sealed()
}

object A
class B

fun sealed(s: Sealed): Int = when (s) {
    <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>A<!> -> 1
    <!USELESS_IS_CHECK!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>B<!><!> -> 2
    C -> 3
    is D -> 4
    else -> 5
}

/* GENERATED_FIR_TAGS: classDeclaration, data, equalityExpression, functionDeclaration, integerLiteral, isExpression,
nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression,
whenWithSubject */
