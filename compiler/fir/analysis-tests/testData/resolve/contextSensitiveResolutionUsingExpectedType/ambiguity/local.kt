// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// FIR_DUMP
// RENDER_DIAGNOSTICS_FULL_TEXT

package foo

sealed class Sealed {
    data object A : Sealed()
    data class B(val x: Int) : Sealed()
    data object C : Sealed()
    data class D(val y: Int) : Sealed()
    data class String(val t: kotlin.String) : Sealed()

    companion object {
        val CompanionA: Sealed = Sealed.A
        val CompanionB: Sealed = Sealed.A
    }
}

fun test(s: Sealed): Int {
    class B
    val CompanionB: Int = 0

    return when (s) {
        <!IMPOSSIBLE_IS_CHECK_WARNING!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>B<!><!> -> 2
        C -> 3
        is D -> 4
        <!IMPOSSIBLE_IS_CHECK_WARNING!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>String<!><!> -> 5
        <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY, INCOMPATIBLE_TYPES!>CompanionB<!> -> 7
        else -> 100
    }
}

fun testSealed(s: Sealed): Int {
    class B
    val CompanionB: Int = 0

    return when (s) {
        is Sealed.B -> 2
        Sealed.C -> 3
        is Sealed.D -> 4
        is Sealed.String -> 5
        Sealed.CompanionB -> 7
        else -> 100
    }
}

class Test {
    object A
    class B

    object CompanionA
    val CompanionB: Int = 0

    fun test(s: Sealed): Int {
        return when (s) {
            <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>A<!> -> 1
            <!IMPOSSIBLE_IS_CHECK_WARNING!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>B<!><!> -> 2
            C -> 3
            is D -> 4
            <!IMPOSSIBLE_IS_CHECK_WARNING!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>String<!><!> -> 5
            <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>CompanionA<!> -> 6
            <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY, INCOMPATIBLE_TYPES!>CompanionB<!> -> 7
            else -> 100
        }
    }

    fun testClass(s: Sealed): Int {
        return when (s) {
            Test.A -> 1
            <!IMPOSSIBLE_IS_CHECK_WARNING!>is Test.B<!> -> 2
            Sealed.C -> 3
            is Sealed.D -> 4
            <!IMPOSSIBLE_IS_CHECK_WARNING!>is kotlin.String<!> -> 5
            Test.CompanionA -> 6
            <!INCOMPATIBLE_TYPES!>this.CompanionB<!> -> 7
            else -> 100
        }
    }

    fun testSealed(s: Sealed): Int {
        return when (s) {
            Sealed.A -> 1
            is Sealed.B -> 2
            Sealed.C -> 3
            is Sealed.D -> 4
            is Sealed.String -> 5
            Sealed.CompanionA -> 6
            Sealed.CompanionB -> 7
            <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 100
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, equalityExpression, functionDeclaration, integerLiteral,
isExpression, localClass, localProperty, nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration, sealed,
smartcast, whenExpression, whenWithSubject */
