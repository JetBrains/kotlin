val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>test1<!> = when {
    true -> { <!TYPE_MISMATCH!>{ true }<!> }
    else -> TODO()
}

val test1a: () -> Boolean = when {
    true -> { { true } }
    else -> TODO()
}

val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>test2<!> = when {
    true -> { <!TYPE_MISMATCH!>{ true }<!> }
    else -> when {
        true -> { <!TYPE_MISMATCH!>{ true }<!> }
        else -> TODO()
    }
}

val test2a: () -> Boolean = when {
    true -> { { true } }
    else -> when {
        true -> { <!TYPE_MISMATCH!>{ true }<!> } // TODO
        else -> TODO()
    }
}

val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>test3<!> = when {
    true -> { <!TYPE_MISMATCH!>{ true }<!> }
    true -> { <!TYPE_MISMATCH!>{ true }<!> }
    else -> TODO()
}

val test3a: () -> Boolean = when {
    true -> { { true } }
    true -> { { true } }
    else -> TODO()
}