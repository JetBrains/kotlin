// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
val test1 = when {
    true -> { { true } }
    else -> TODO()
}

val test1a: () -> Boolean = when {
    true -> { { true } }
    else -> TODO()
}

val test2 = when {
    true -> { { true } }
    else -> when {
        true -> { { true } }
        else -> TODO()
    }
}

val test2a: () -> Boolean = when {
    true -> { { true } }
    else -> when {
        true -> { { true } } // TODO
        else -> TODO()
    }
}

val test3 = when {
    true -> { { true } }
    true -> { { true } }
    else -> TODO()
}

val test3a: () -> Boolean = when {
    true -> { { true } }
    true -> { { true } }
    else -> TODO()
}