// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: foo.kt

package foo

object A
class B

// FILE: bar.kt

package bar

sealed class Sealed {
    data object A : Sealed()
    data class B(val x: Int) : Sealed()
    data object C : Sealed()
    data class D(val y: Int) : Sealed()
}

// FILE: main.kt

import foo.*
import bar.*

fun sealed(s: Sealed): Int = when (s) {
    <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>A<!> -> 1
    <!USELESS_IS_CHECK!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>B<!><!> -> 2
    C -> 3
    is D -> 4
    else -> 5
}

fun sealedExplicit(s: Sealed): Int = when (s) {
    bar.Sealed.A -> 1
    is bar.Sealed.B -> 2
    else -> 5
}

fun topLevelExplicit(s: Sealed): Int = when (s) {
    foo.A -> 1
    <!USELESS_IS_CHECK!>is foo.B<!> -> 2
    else -> 5
}

/* GENERATED_FIR_TAGS: classDeclaration, data, equalityExpression, functionDeclaration, integerLiteral, isExpression,
nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression,
whenWithSubject */
