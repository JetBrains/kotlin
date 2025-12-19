// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
sealed class Base {
    class A : Base() {
        class B : Base()
    }
}

class C : Base()

fun test_1(e: Base) {
    val a = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        is Base.A -> 1
        is Base.A.B -> 2
    }

    val b = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        is Base.A -> 1
        is Base.A.B -> 2
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is String<!> -> 3
    }

    val c = <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        is Base.A -> 1
        is Base.A.B -> 2
        is C -> 3
    }<!>

    val d = <!WHEN_ON_SEALED_EEN_EN_ELSE!>when (e) {
        is Base.A -> 1
        else -> 2
    }<!>
}

fun test_2(e: Base?) {
    val a = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        is Base.A -> 1
        is Base.A.B -> 2
        is C -> 3
    }

    val b = <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        is Base.A -> 1
        is Base.A.B -> 2
        is C -> 3
        null -> 4
    }<!>

    val c = <!WHEN_ON_SEALED_WEL_ELSE!>when (e) {
        is Base.A -> 1
        is Base.A.B -> 2
        is C -> 3
        else -> 4
    }<!>
}

fun test_3(e: Base) {
    val a = <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        is Base.A, is Base.A.B -> 1
        is C -> 2
    }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, disjunctionExpression, equalityExpression, functionDeclaration, integerLiteral,
isExpression, localProperty, nestedClass, nullableType, propertyDeclaration, sealed, smartcast, whenExpression,
whenWithSubject */
