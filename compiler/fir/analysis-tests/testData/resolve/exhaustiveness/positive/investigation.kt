// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// LANGUAGE: +DataFlowBasedExhaustiveness

public enum class X {
    A, B, C;
}

operator fun Int.contains(x: X): Boolean = true

fun foo(x : X) {
    val a = if (x == X.A) 1 else 2
    val b1 = <!WHEN_ON_SEALED("X; SPECIAL_CASE")!>when (x) {
        X.A -> 1
        else -> 100
    }<!>
    val b2 = <!WHEN_ON_SEALED("X; OTHER")!>when (x) {
        X.A -> 1
        X.B -> 2
        else -> 100
    }<!>
    val c = <!WHEN_ON_SEALED("X; REDUNDANT")!>when (x) {
        X.A -> 1
        X.B -> 2
        X.C -> 3
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 100
    }<!>
    val f = <!WHEN_ON_SEALED("X; EXHAUSTIVE")!>when (x) {
        X.A -> 1
        X.B -> 2
        X.C -> 3
    }<!>
    val g = <!WHEN_ON_SEALED("X; RANGE")!>when (x) {
        in 3 -> 1
        else -> 100
    }<!>
    val h = <!WHEN_ON_SEALED("X; RANGE")!>when (x) {
        X.A -> 1
        in 3 -> 2
        else -> 100
    }<!>
    val d = <!WHEN_ON_SEALED("X; REDUNDANT+THROW")!>when (x) {
        X.A -> 1
        X.B -> 2
        X.C -> 3
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> throw IllegalStateException()
    }<!>
}

fun foo1(x: X) {
    val e1 = <!WHEN_ON_SEALED("X; SPECIAL_CASE+THROW")!>when (x) {
        X.A -> 1
        else -> throw IllegalStateException()
    }<!>
}

fun foo2(x: X) {
    val e2 = <!WHEN_ON_SEALED("X; OTHER+THROW")!>when (x) {
        X.A -> 1
        X.B -> 2
        else -> throw IllegalStateException()
    }<!>
}

fun foo3(x: X) {
    val e1 = <!WHEN_ON_SEALED("X; SPECIAL_CASE+RETURN")!>when (x) {
        X.A -> 1
        else -> return
    }<!>
}

fun foo4(x: X) {
    val e2 = <!WHEN_ON_SEALED("X; OTHER+RETURN")!>when (x) {
        X.A -> 1
        X.B -> 2
        else -> return
    }<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
localProperty, propertyDeclaration, whenExpression, whenWithSubject */
