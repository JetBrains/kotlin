// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// FIR_DUMP

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
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is B<!> -> 2
        C -> 3
        is D -> 4
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>String<!><!> -> 5
        <!INCOMPATIBLE_TYPES!>CompanionB<!> -> 7
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
            A -> 1
            <!IMPOSSIBLE_IS_CHECK_ERROR!>is B<!> -> 2
            C -> 3
            is D -> 4
            <!IMPOSSIBLE_IS_CHECK_ERROR!>is <!CONTEXT_SENSITIVE_RESOLUTION_AMBIGUITY!>String<!><!> -> 5
            CompanionA -> 6
            <!INCOMPATIBLE_TYPES!>CompanionB<!> -> 7
            else -> 100
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, equalityExpression, functionDeclaration, integerLiteral,
isExpression, localClass, localProperty, nestedClass, objectDeclaration, primaryConstructor, propertyDeclaration, sealed,
smartcast, whenExpression, whenWithSubject */
