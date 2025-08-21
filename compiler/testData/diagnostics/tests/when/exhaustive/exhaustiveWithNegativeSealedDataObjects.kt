// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +DataFlowBasedExhaustiveness

sealed interface Variants {
    data object A : Variants
    data object B : Variants
    data object C : Variants {
        fun test(): String = "C test"
    }
}

fun foo(v: Variants): String {
    if (v is Variants.A) {
        return "A"
    }

    return <!NO_ELSE_IN_WHEN!>when<!> (v) {
        Variants.B -> "B"
        Variants.C -> "C"
    }
}

fun bar(v: Variants): String {
    if (v is Variants.A) {
        return "A"
    }

    if (v is Variants.B) {
        return "B"
    }

    return <!NO_ELSE_IN_WHEN!>when<!> (v) {
        Variants.C -> "C"
    }
}

fun baz(v: Variants): String {
    if (v is Variants.A) {
        return "A"
    }

    return when (v) {
        Variants.B -> "B"
        else -> v.<!UNRESOLVED_REFERENCE!>test<!>()
    }
}

fun quux(v: Variants): String {
    if (v is Variants.A) {
        return "A"
    }

    if (v is Variants.B) {
        return "B"
    }

    return v.<!UNRESOLVED_REFERENCE!>test<!>()
}

/* GENERATED_FIR_TAGS: data, equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
nestedClass, objectDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
