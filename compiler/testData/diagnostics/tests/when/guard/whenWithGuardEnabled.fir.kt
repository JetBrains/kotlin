// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN
// FIR_DUMP

sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

class StringHolder(val value: String?)

fun SmartCastInGuard(x: Any) {
    return when (x) {
        is String -> Unit
        is BooleanHolder if x.value -> Unit
        is StringHolder if x.value != null &&
                x.value.length > 0 -> Unit
        else if x is Boolean && x -> Unit
        else -> Unit
    }
}

fun GuardWithoutSubject(x: Any) {
    return when {
        x is String -> Unit
        x is BooleanHolder <!WHEN_GUARD_WITHOUT_SUBJECT!>if x.value<!> -> Unit
        if (x is Boolean) true else false -> Unit
        else <!WHEN_GUARD_WITHOUT_SUBJECT!>if x is Boolean<!> -> Unit
        else -> Unit
    }
}

fun MultipleConditionsNotAllowed(x: Any) {
    return when (x) {
        is String -> Unit
        is True, is False <!COMMA_IN_WHEN_CONDITION_WITH_WHEN_GUARD!>if x.value<!> -> Unit
        else -> Unit
    }
}

fun NonExhaustiveWithGuard(x: BooleanHolder) {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is True -> Unit
        is False if x.value -> Unit
    }
}

fun ExhaustiveWithGuard(x: BooleanHolder) {
    return when (x) {
        is True -> Unit
        is False if x.value -> Unit
        is False -> Unit
    }
}
