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

    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (v) {
        is Variants.B -> "B"
        is Variants.C -> "C"
    }<!>
}

fun bar(v: Variants): String {
    if (v is Variants.A) {
        return "A"
    }

    if (v is Variants.B) {
        return "B"
    }

    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (v) {
        is Variants.C -> "C"
    }<!>
}

fun baz(v: Variants): String {
    if (v is Variants.A) {
        return "A"
    }

    return <!WHEN_ON_SEALED_EEN_EN_ELSE!>when (v) {
        is Variants.B -> "B"
        else -> v.<!UNRESOLVED_REFERENCE!>test<!>()
    }<!>
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

/* GENERATED_FIR_TAGS: data, functionDeclaration, ifExpression, interfaceDeclaration, isExpression, nestedClass,
objectDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
