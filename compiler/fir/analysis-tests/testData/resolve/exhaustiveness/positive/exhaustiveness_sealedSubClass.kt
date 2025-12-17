// RUN_PIPELINE_TILL: FRONTEND
sealed class A

sealed class B : A()
class C : A()

sealed class D : B()
sealed class E : B()

fun test_1(e: A) {
    val a = <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        is C -> 1
        is D -> 2
        is E -> 3
    }<!>.plus(0)

    val b = <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        is B -> 1
        is C -> 2
    }<!>.plus(0)

    val c = <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        is B -> 1
        is C -> 2
        is E -> 3
        is D -> 4
    }<!>.plus(0)

    val d = <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        is E -> 1
        is A -> 2
    }<!>.plus(0)
}

fun test_2(e: A) {
    val a = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        is D -> 1
        is E -> 2
    }.plus(0)

    val b = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        is B -> 1
        is D -> 2
        is E -> 3
    }.plus(0)

    val c = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        is B -> 1
        is D -> 2
    }.plus(0)

    val d = <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        is C -> 1
    }<!>.plus(0)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, isExpression, localProperty,
propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
