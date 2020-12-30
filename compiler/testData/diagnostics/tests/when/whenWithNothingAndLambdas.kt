// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 2 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * declarations, function-declaration -> paragraph 7 -> sentence 1
 * declarations, function-declaration -> paragraph 7 -> sentence 2
 * declarations, function-declaration -> paragraph 8 -> sentence 1
 * overload-resolution, determining-function-applicability-for-a-specific-call, description -> paragraph 1 -> sentence 3
 */

val <!IMPLICIT_NOTHING_PROPERTY_TYPE{OI}!>test1<!> = when {
    true -> { <!TYPE_MISMATCH{OI}!>{ true }<!> }
    else -> TODO()
}

val test1a: () -> Boolean = when {
    true -> { { true } }
    else -> TODO()
}

val <!IMPLICIT_NOTHING_PROPERTY_TYPE{OI}!>test2<!> = when {
    true -> { <!TYPE_MISMATCH{OI}!>{ true }<!> }
    else -> when {
        true -> { <!TYPE_MISMATCH{OI}!>{ true }<!> }
        else -> TODO()
    }
}

val test2a: () -> Boolean = when {
    true -> { { true } }
    else -> when {
        true -> { <!TYPE_MISMATCH{OI}!>{ true }<!> } // TODO
        else -> TODO()
    }
}

val <!IMPLICIT_NOTHING_PROPERTY_TYPE{OI}!>test3<!> = when {
    true -> { <!TYPE_MISMATCH{OI}!>{ true }<!> }
    true -> { <!TYPE_MISMATCH{OI}!>{ true }<!> }
    else -> TODO()
}

val test3a: () -> Boolean = when {
    true -> { { true } }
    true -> { { true } }
    else -> TODO()
}
