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

fun cast1wrong(s: Sealed): Int {
    s <!CAST_NEVER_SUCCEEDS!>as<!> <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>A<!>
    return 1
}

fun cast2wrong(s: Sealed): Int {
    s <!CAST_NEVER_SUCCEEDS!>as<!> <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>B<!>
    return 2
}

fun cast1sealed(s: Sealed): Int {
    s as Sealed.A
    return 1
}

fun cast2sealed(s: Sealed): Int {
    s as Sealed.B
    return 2
}

fun cast1topLevel(s: Sealed): Int {
    s <!CAST_NEVER_SUCCEEDS!>as<!> foo.A
    return 1
}

fun cast2topLevel(s: Sealed): Int {
    s <!CAST_NEVER_SUCCEEDS!>as<!> foo.B
    return 2
}

fun cast3ok(s: Sealed): Int {
    s as C
    return 3
}

fun cast4ok(s: Sealed): Int {
    s as D
    return 4
}

fun equality1wrong(s: Sealed): Boolean = s == <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>A<!>
fun equality1sealed(s: Sealed): Boolean = s == Sealed.A
fun equality1topLevel(s: Sealed): Boolean = s == foo.A
fun equality2ok(s: Sealed): Boolean = s == Sealed.C

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, data, equalityExpression, functionDeclaration, integerLiteral,
isExpression, nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression,
whenWithSubject */
