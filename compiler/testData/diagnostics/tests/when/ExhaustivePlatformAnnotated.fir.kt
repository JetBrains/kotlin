// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71490

// FILE: J.java

import org.jetbrains.annotations.*;

public enum J {
    A, B;

    public static J getPlatform() {
        return J.A;
    }

    @NotNull public static J getNotNull() {
        return J.A;
    }

    @Nullable public static J getNullable() {
        return J.A;
    }
}

// FILE: K.kt

fun test_1(): Int {
    return <!WHEN_ON_SEALED!>when (J.getPlatform()) {
        J.A -> 1
        J.B -> 2
    }<!>
}

fun test_2(): Int {
    return <!WHEN_ON_SEALED!>when (J.getPlatform()) {
        J.A -> 1
        J.B -> 2
        null -> 3
    }<!>
}

fun test_3(): Int {
    return <!WHEN_ON_SEALED!>when (J.getPlatform()) {
        J.A -> 1
        J.B -> 2
        else -> 3
    }<!>
}

fun test_4(): Int {
    return <!WHEN_ON_SEALED!>when (J.getPlatform()) {
        J.A -> 1
        J.B -> 2
        null -> 3
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 4
    }<!>
}

fun test_5(): Int {
    return <!WHEN_ON_SEALED!>when (J.getNotNull()) {
        J.A -> 1
        J.B -> 2
    }<!>
}

fun test_6(): Int {
    return <!WHEN_ON_SEALED!>when (J.getNotNull()) {
        J.A -> 1
        J.B -> 2
        <!SENSELESS_NULL_IN_WHEN!>null<!> -> 3
    }<!>
}

fun test_7(): Int {
    return <!WHEN_ON_SEALED!>when (J.getNotNull()) {
        J.A -> 1
        J.B -> 2
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 3
    }<!>
}

fun test_8(): Int {
    return <!WHEN_ON_SEALED!>when (J.getNotNull()) {
        J.A -> 1
        J.B -> 2
        <!SENSELESS_NULL_IN_WHEN!>null<!> -> 3
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 4
    }<!>
}


fun test_9(): Int {
    return <!NO_ELSE_IN_WHEN!>when<!> (J.getNullable()) {
        J.A -> 1
        J.B -> 2
    }
}

fun test_10(): Int {
    return <!WHEN_ON_SEALED!>when (J.getNullable()) {
        J.A -> 1
        J.B -> 2
        null -> 3
    }<!>
}

fun test_11(): Int {
    return <!WHEN_ON_SEALED!>when (J.getNullable()) {
        J.A -> 1
        J.B -> 2
        else -> 3
    }<!>
}

fun test_12(): Int {
    return <!WHEN_ON_SEALED!>when (J.getNullable()) {
        J.A -> 1
        J.B -> 2
        null -> 3
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> 4
    }<!>
}

/* GENERATED_FIR_TAGS: equalityExpression, flexibleType, functionDeclaration, integerLiteral, javaProperty, javaType,
nullableType, smartcast, whenExpression, whenWithSubject */
