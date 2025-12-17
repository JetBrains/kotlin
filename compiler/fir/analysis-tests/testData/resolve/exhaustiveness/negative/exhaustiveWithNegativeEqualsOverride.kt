// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +DataFlowBasedExhaustiveness

sealed class SealedWithEquals {
    object A : SealedWithEquals()
    object B : SealedWithEquals()
    object C : SealedWithEquals()

    override fun equals(other: Any?): Boolean {
        return this === other
    }
}

fun whenWithEquals(e: SealedWithEquals): Int {
    if (e == SealedWithEquals.A) return 1
    if (e == SealedWithEquals.B) return 2

    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        SealedWithEquals.A -> 3
        SealedWithEquals.B -> 4
        SealedWithEquals.C -> 5
    }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
nestedClass, nullableType, objectDeclaration, operator, override, sealed, smartcast, thisExpression, whenExpression,
whenWithSubject */
