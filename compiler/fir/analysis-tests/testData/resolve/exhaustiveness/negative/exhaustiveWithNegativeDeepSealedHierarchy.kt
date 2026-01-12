// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +DataFlowBasedExhaustiveness

sealed interface Variants {
    data object A : Variants

    sealed interface Subvariants : Variants {
        data object B : Subvariants
    }
}

fun foo(v: Variants): String {
    if (v is Variants.Subvariants) {
        return "B"
    }

    return <!WHEN_ON_SEALED!>when (v) {
        is Variants.A -> "A"
    }<!>
}

/* GENERATED_FIR_TAGS: data, functionDeclaration, ifExpression, interfaceDeclaration, isExpression, nestedClass,
objectDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
