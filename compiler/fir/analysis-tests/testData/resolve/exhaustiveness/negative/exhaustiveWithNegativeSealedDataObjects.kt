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
        Variants.B -> "B"
        Variants.C -> "C"
    }<!>
}

fun simpleSealed1 (v: Variants): String {
    if (v is Variants.A) {
        return "A"
    }

    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (v) {
        Variants.B, Variants.C -> "B, C"
    }<!>
}

fun simpleSealed2(x: Variants): Int {
    if (x is Variants.C || x is Variants.A) return 1
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        Variants.B -> 3
    }<!>
}

fun negSimpleSealed(x: Variants): Int {
    if (x !is Variants.C) return 0
    return when (x) {
        Variants.C -> 1
    }
}

fun simpleSealedThrow(x: Variants): Int {
    if (x is Variants.A) throw IllegalArgumentException("A")
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        Variants.B -> 2
        Variants.C -> 3
    }<!>
}

fun simpleSealedThrow2(x: Variants): Int {
    (x is Variants.A) && throw IllegalArgumentException("A")
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        Variants.B -> 2
        Variants.C -> 3
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
        Variants.C -> "C"
    }<!>
}

fun baz(v: Variants): String {
    if (v is Variants.A) {
        return "A"
    }

    return <!WHEN_ON_SEALED_EEN_EN_ELSE!>when (v) {
        Variants.B -> "B"
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

/* GENERATED_FIR_TAGS: data, equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
nestedClass, objectDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
