// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: foo.kt

package foo

object A
class B
class E
object F

object CompanionA
val CompanionB: Int = 0

// FILE: bar.kt

package bar

sealed class Sealed {
    data object A : Sealed()
    data class B(val x: Int) : Sealed()
    data object C : Sealed()
    data class D(val y: Int) : Sealed()
    data class E(val z: Int) : Sealed()
    data object F : Sealed()
    data class String(val t: kotlin.String) : Sealed()

    companion object {
        val CompanionA: Sealed = Sealed.A
        val CompanionB: Sealed = Sealed.A
    }
}

// FILE: main.kt

import foo.*
import foo.E
import foo.F
import bar.*

fun sealed(s: Sealed): Int = when (s) {
    <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>A<!> -> 1
    <!IMPOSSIBLE_IS_CHECK_ERROR!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>B<!><!> -> 2
    C -> 3
    is D -> 4
    <!IMPOSSIBLE_IS_CHECK_ERROR!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>String<!><!> -> 5
    <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>CompanionA<!> -> 6
    <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY, INCOMPATIBLE_TYPES!>CompanionB<!> -> 7
    <!IMPOSSIBLE_IS_CHECK_ERROR!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>E<!><!> -> 8
    <!IMPOSSIBLE_IS_CHECK_ERROR!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>F<!><!> -> 9
    else -> 100
}

fun sealedExplicit(s: Sealed): Int = when (s) {
    bar.Sealed.A -> 1
    is bar.Sealed.B -> 2
    is bar.Sealed.String -> 5
    bar.Sealed.CompanionA -> 6
    bar.Sealed.CompanionB -> 7
    is bar.Sealed.E -> 8
    is bar.Sealed.F -> 9
    else -> 100
}

fun topLevelExplicit(s: Sealed): Int = when (s) {
    foo.A -> 1
    <!IMPOSSIBLE_IS_CHECK_ERROR!>is foo.B<!> -> 2
    <!IMPOSSIBLE_IS_CHECK_ERROR!>is kotlin.String<!> -> 5
    foo.CompanionA -> 6
    <!INCOMPATIBLE_TYPES!>foo.CompanionB<!> -> 7
    <!IMPOSSIBLE_IS_CHECK_ERROR!>is foo.E<!> -> 8
    <!IMPOSSIBLE_IS_CHECK_ERROR!>is foo.F<!> -> 9
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

fun equality3wrong(s: Sealed): Boolean = s == <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>CompanionA<!>
fun equality3sealed(s: Sealed): Boolean = s == Sealed.CompanionA
fun equality3topLevel(s: Sealed): Boolean = s == foo.CompanionA

fun equality4ok(s: Sealed): Boolean = s == <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>F<!>
fun equality4sealed(s: Sealed): Boolean = s == Sealed.F
fun equality4topLevel(s: Sealed): Boolean = s == foo.F

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, data, equalityExpression, functionDeclaration, integerLiteral,
isExpression, nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression,
whenWithSubject */
