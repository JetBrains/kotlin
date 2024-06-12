// LANGUAGE: +WhenGuards
// WITH_STDLIB
// DIAGNOSTICS: -SENSELESS_COMPARISON, -USELESS_IS_CHECK, -DUPLICATE_LABEL_IN_WHEN

sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

fun nonExhaustiveWithGuard(x: BooleanHolder) {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is True -> Unit
        is False if x.value -> Unit
    }
}

fun falsePositiveNonExhaustive(y: BooleanHolder) {
    <!NO_ELSE_IN_WHEN!>when<!> (y) {
        is True if y.value -> Unit
        is False if y.value -> Unit
    }
}

fun nonExhaustiveWithElseIf(x: BooleanHolder) {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is True -> Unit
        else if true -> Unit
    }
}

fun exhaustiveWithGuard(x: BooleanHolder) {
    return when (x) {
        is True -> Unit
        is False if x.value -> Unit
        is False -> Unit
    }
}

fun exhaustiveWithElseIf(x: BooleanHolder) {
    return when (x) {
        is True -> Unit
        else if true -> Unit
        else -> Unit
    }
}

fun falseNegativeNotExhaustive(x: Any) {
    val w = <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is Int if { _ : String -> <!EQUALITY_NOT_APPLICABLE_WARNING!>x == "10"<!> } ("11") -> println('q')
    }
}