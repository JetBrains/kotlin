// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
/*
 * RELEVANT SPEC SENTENCES (spec version: 0.1-152, test type: neg):
 *  - expressions, when-expression -> paragraph 2 -> sentence 1
 *  - expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 *  - declarations, function-declaration -> paragraph 7 -> sentence 1
 *  - declarations, function-declaration -> paragraph 7 -> sentence 2
 *  - declarations, function-declaration -> paragraph 8 -> sentence 1
 *  - overload-resolution, determining-function-applicability-for-a-specific-call, description -> paragraph 1 -> sentence 3
 */

val <!OI;IMPLICIT_NOTHING_PROPERTY_TYPE!>test1<!> = when {
    true -> { <!OI;TYPE_MISMATCH!>{ true }<!> }
    else -> TODO()
}

val test1a: () -> Boolean = when {
    true -> { { true } }
    else -> TODO()
}

val <!OI;IMPLICIT_NOTHING_PROPERTY_TYPE!>test2<!> = when {
    true -> { <!OI;TYPE_MISMATCH!>{ true }<!> }
    else -> when {
        true -> { <!OI;TYPE_MISMATCH!>{ true }<!> }
        else -> TODO()
    }
}

val test2a: () -> Boolean = when {
    true -> { { true } }
    else -> when {
        true -> { <!OI;TYPE_MISMATCH!>{ true }<!> } // TODO
        else -> TODO()
    }
}

val <!OI;IMPLICIT_NOTHING_PROPERTY_TYPE!>test3<!> = when {
    true -> { <!OI;TYPE_MISMATCH!>{ true }<!> }
    true -> { <!OI;TYPE_MISMATCH!>{ true }<!> }
    else -> TODO()
}

val test3a: () -> Boolean = when {
    true -> { { true } }
    true -> { { true } }
    else -> TODO()
}