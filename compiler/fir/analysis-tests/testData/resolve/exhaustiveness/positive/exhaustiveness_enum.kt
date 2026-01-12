// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
enum class Enum {
    A, B, C
}

fun test_1(e: Enum) {
    val a = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        Enum.A -> 1
        Enum.B -> 2
    }

    val b = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        Enum.A -> 1
        Enum.B -> 2
        <!IMPOSSIBLE_IS_CHECK_WARNING!>is String<!> -> 3
    }

    val c = <!WHEN_ON_SEALED!>when (e) {
        Enum.A -> 1
        Enum.B -> 2
        Enum.C -> 3
    }<!>

    val d = <!WHEN_ON_SEALED!>when (e) {
        Enum.A -> 1
        else -> 2
    }<!>
}

fun test_2(e: Enum?) {
    val a = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        Enum.A -> 1
        Enum.B -> 2
        Enum.C -> 3
    }

    val b = <!WHEN_ON_SEALED!>when (e) {
        Enum.A -> 1
        Enum.B -> 2
        Enum.C -> 3
        null -> 4
    }<!>

    val c = <!WHEN_ON_SEALED!>when (e) {
        Enum.A -> 1
        Enum.B -> 2
        Enum.C -> 3
        else -> 4
    }<!>
}

fun test_3(e: Enum) {
    val a = <!WHEN_ON_SEALED!>when (e) {
        Enum.A, Enum.B -> 1
        Enum.C -> 2
    }<!>
}

/* GENERATED_FIR_TAGS: disjunctionExpression, enumDeclaration, enumEntry, equalityExpression, functionDeclaration,
integerLiteral, isExpression, localProperty, nullableType, propertyDeclaration, smartcast, whenExpression,
whenWithSubject */
