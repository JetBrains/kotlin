// !LANGUAGE: +NewInference

fun <T, R, vararg Ts> T.receiverWithOthers (
    vararg others: *Ts,
    transform: (T, *Ts) -> R
): R {
    return transform(this, others)
}

fun <R, vararg Ts> justOthers (
    vararg others: *Ts,
    transform: (*Ts) -> R
): R {
    return transform(others)
}

val mismatched = Unit.receiverWithOthers <!TYPE_MISMATCH!>{
    <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!><!>->
}<!>

val ok = justOthers { ->
    "Fine"
}

val noIt = justOthers {
    <!UNRESOLVED_REFERENCE!>it<!>
}