// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78068
// LANGUAGE: +DataFlowBasedExhaustiveness

enum class Enum { A, B, C }

fun foo(e: Enum): Int {
    if (e == Enum.A) return 1
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        Enum.B -> 2
        Enum.C -> 3
    }<!>
}

fun bar(e: Enum): Int {
    if (e == Enum.A) return 1
    if (e == Enum.B) return 2
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        Enum.C -> 3
    }<!>
}

fun simpleEnum(x: Enum): Int {
    if (x == Enum.C) return 1
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        Enum.A, Enum.B -> 3
    }<!>
}

fun negSimpleEnum(x: Enum): Int {
    if (x != Enum.C) return 0
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) { // KT-78068
        Enum.C -> 3
    }<!>
}

fun simpleEnumThrow(x: Enum): Int {
    if (x == Enum.C) throw AssertionError("C")
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        Enum.A -> 2
        Enum.B -> 3
    }<!>
}

fun simpleEnumThrow2(x: Enum): Int {
    if (x == Enum.A) throw IllegalArgumentException("A")
    if (x == Enum.B) throw IllegalArgumentException("B")
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        Enum.C -> 3
    }<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
smartcast, whenExpression, whenWithSubject */
