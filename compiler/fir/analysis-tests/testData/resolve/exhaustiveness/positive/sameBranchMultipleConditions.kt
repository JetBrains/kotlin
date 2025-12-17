// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-46301

sealed interface A
sealed interface B

data class X(val something: String): A, B
data class Y(val something: String): A, B

fun ok(a: A): B {
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (a) {
        is X -> a
        is Y -> a
    }<!>
}

fun problem(a: A): B {
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (a) {
        is X, is Y -> a
    }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, data, disjunctionExpression, functionDeclaration, interfaceDeclaration,
intersectionType, isExpression, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression,
whenWithSubject */
