// !WITH_NEW_INFERENCE
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