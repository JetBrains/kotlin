// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

package foo

sealed class Sealed {
    data object A : Sealed()
    data class B(val x: Int) : Sealed()
    data object C : Sealed()
    data class D(val y: Int) : Sealed()
    data class String(val t: kotlin.String) : Sealed()
}

object A
class B

fun sealed(s: Sealed): Int = when (s) {
    <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>A<!> -> 1
    <!IMPOSSIBLE_IS_CHECK_ERROR!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>B<!><!> -> 2
    C -> 3
    is D -> 4
    <!IMPOSSIBLE_IS_CHECK_ERROR!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>String<!><!> -> 5
    else -> 6
}

fun sealedExplicit(s: Sealed): Int = when (s) {
    Sealed.A -> 1
    is Sealed.B -> 2
    else -> 6
}

fun topLevelExplicit(s: Sealed): Int = when (s) {
    foo.A -> 1
    <!IMPOSSIBLE_IS_CHECK_ERROR!>is foo.B<!> -> 2
    else -> 6
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
