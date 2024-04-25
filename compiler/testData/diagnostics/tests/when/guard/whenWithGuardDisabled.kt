// LANGUAGE: -WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN

sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

class StringHolder(val value: String?)

fun SmartCastInGuard(x: Any) {
    return when (x) {
        is String -> Unit
        is BooleanHolder <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        is StringHolder <!UNSUPPORTED_FEATURE!>if x.value != null &&
                x.value.length > 0<!> -> Unit
        else <!UNSUPPORTED_FEATURE!>if x is Boolean && x<!> -> Unit
        else -> Unit
    }
}

fun GuardWithoutSubject(x: Any) {
    return when {
        x is String -> Unit
        x is BooleanHolder <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        if (x is Boolean) true else false -> Unit
        else <!UNSUPPORTED_FEATURE!>if x is Boolean<!> -> Unit
        else -> Unit
    }
}

fun MultipleConditionsNotAllowed(x: Any) {
    return when (x) {
        is String -> Unit
        is True, is False <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        else -> Unit
    }
}

fun NonExhaustiveWithGuard(x: BooleanHolder) {
    return when (x) {
        is True -> Unit
        is False <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
    }
}

fun ExhaustiveWithGuard(x: BooleanHolder) {
    return when (x) {
        is True -> Unit
        is False <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        is False -> Unit
    }
}
