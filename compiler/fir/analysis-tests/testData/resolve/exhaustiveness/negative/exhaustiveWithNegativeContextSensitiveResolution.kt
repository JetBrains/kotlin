// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +DataFlowBasedExhaustiveness, +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum { A, B, C }

sealed interface MySealedInterface {
    object A : MySealedInterface
    object B : MySealedInterface
    object C : MySealedInterface
}

fun simpleSealedCSR(x: MySealedInterface): Int {
    if (x is A) return 1
    return <!WHEN_ON_SEALED!>when (x) {
        B -> 2
        C -> 3
    }<!>
}

fun simpleEnumCSR(x: MyEnum): Int {
    if (x == C) return 1
    return <!WHEN_ON_SEALED!>when (x) {
        A -> 2
        B -> 3
    }<!>
}

fun simpleEnumCSR2(x: MyEnum): Int {
    if (x == C) return 1
    return <!WHEN_ON_SEALED!>when (x) {
        A, B -> 3
    }<!>
}

fun noSubjectSealedCSR(x: MySealedInterface): Int {
    if (x is A) return 1
    return <!NO_ELSE_IN_WHEN!>when<!> {
        x is B -> 2
        x is C -> 3
    }
}

/* GENERATED_FIR_TAGS: disjunctionExpression, enumDeclaration, enumEntry, equalityExpression, functionDeclaration,
ifExpression, integerLiteral, interfaceDeclaration, isExpression, nestedClass, objectDeclaration, sealed, whenExpression,
whenWithSubject */
