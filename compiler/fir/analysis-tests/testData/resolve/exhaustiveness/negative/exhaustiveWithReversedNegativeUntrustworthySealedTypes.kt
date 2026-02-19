// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +DataFlowBasedExhaustiveness

open class PhantomEquivalence {
    override fun equals(other: Any?) = other is PhantomEquivalence
}

sealed interface Variants {
    object A : Variants
    object B : PhantomEquivalence(), Variants
    data object C : Variants
    data object D : PhantomEquivalence(), Variants
}

fun foo(v: Variants): String {
    if (v == Variants.B) {
        return "B"
    }

    return when (v) {
        Variants.A -> "B"
        Variants.D -> "D"
        Variants.C -> "C"
    }
}

fun bar(v: Variants): String {
    if (v == Variants.B) {
        return "B"
    }

    return when (v) {
        Variants.A -> "A"
        Variants.D -> "D"
        Variants.C -> "C"
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> "B?"
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, equalityExpression, functionDeclaration, ifExpression,
interfaceDeclaration, isExpression, nestedClass, nullableType, objectDeclaration, operator, override, sealed, smartcast,
stringLiteral, whenExpression, whenWithSubject */
