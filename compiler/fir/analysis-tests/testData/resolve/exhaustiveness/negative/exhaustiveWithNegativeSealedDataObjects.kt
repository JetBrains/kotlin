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

    return when (v) {
        Variants.B -> "B"
        Variants.C -> "C"
    }
}

fun simpleSealed1 (v: Variants): String {
    if (v is Variants.A) {
        return "A"
    }

    return when (v) {
        Variants.B, Variants.C -> "B, C"
    }
}

fun simpleSealed2(x: Variants): Int {
    if (x is Variants.C || x is Variants.A) return 1
    return when (x) {
        Variants.B -> 3
    }
}

fun negSimpleSealed(x: Variants): Int {
    if (x !is Variants.C) return 0
    return when (x) {
        Variants.C -> 1
    }
}

fun simpleSealedThrow(x: Variants): Int {
    if (x is Variants.A) throw IllegalArgumentException("A")
    return when (x) {
        Variants.B -> 2
        Variants.C -> 3
    }
}

fun simpleSealedThrow2(x: Variants): Int {
    (x is Variants.A) && throw IllegalArgumentException("A")
    return when (x) {
        Variants.B -> 2
        Variants.C -> 3
    }
}

fun bar(v: Variants): String {
    if (v is Variants.A) {
        return "A"
    }

    if (v is Variants.B) {
        return "B"
    }

    return when (v) {
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
