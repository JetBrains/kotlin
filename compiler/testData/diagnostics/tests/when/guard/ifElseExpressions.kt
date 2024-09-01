// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN, -USELESS_IS_CHECK

fun EEInConditionWithNoSubject(x: Any) {
    return when {
        if (x is Boolean) true else false -> Unit
        if (x is Boolean) true else if (x is String) true else false -> Unit
        if (x is Boolean) x == false || if (x is String) true else false else if (x is String) true else false -> Unit
        else -> Unit
    }
}

fun IEEInConditionWithGuard(x: Any) {
    return when (x) {
        if (x is Boolean) true else false <!UNSUPPORTED_FEATURE!>if true<!> -> Unit
        if (x is Boolean) true else if (x is String) true else false <!UNSUPPORTED_FEATURE!>if true<!> -> Unit
        if (x is Boolean) x == false || if (x is String) true else false else if (x is String) true else false <!UNSUPPORTED_FEATURE!>if true<!> -> Unit
        else -> Unit
    }
}

fun WronglyPlacedConditionTypeMismatch(x: Any, y: Any?): Int {
    return when (x) {
        true <!UNSUPPORTED_FEATURE!>if x<!> -> 10
        else -> 0
    }
}

fun IEEInGuard(x: Any) {
    return when (x) {
        is Boolean <!UNSUPPORTED_FEATURE!>if if (x is Boolean) true else false<!> -> Unit
        is Boolean <!UNSUPPORTED_FEATURE!>if if (x is Boolean) true else if (x is String) true else false<!> -> Unit
        is Boolean <!UNSUPPORTED_FEATURE!>if if (x is Boolean) x == false || if (x is String) true else false else if (x is String) true else false<!> -> Unit
        else -> Unit
    }
}
