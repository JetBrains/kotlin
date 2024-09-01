// LANGUAGE: +WhenGuards
// WITH_STDLIB
// DIAGNOSTICS: -SENSELESS_COMPARISON, -USELESS_IS_CHECK, -DUPLICATE_LABEL_IN_WHEN

sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

fun nonExhaustiveWithGuard(x: BooleanHolder) {
    return when (x) {
        is True -> Unit
        is False <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
    }
}

fun falsePositiveNonExhaustive(y: BooleanHolder) {
    when (y) {
        is True <!UNSUPPORTED_FEATURE!>if y.value<!> -> Unit
        is False <!UNSUPPORTED_FEATURE!>if y.value<!> -> Unit
    }
}

fun nonExhaustiveWithElseIf(x: BooleanHolder) {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is True -> Unit
        else <!UNSUPPORTED_FEATURE!>if true<!> -> Unit
    }
}

fun exhaustiveWithGuard(x: BooleanHolder) {
    return when (x) {
        is True -> Unit
        is False <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        is False -> Unit
    }
}

fun exhaustiveWithElseIf(x: BooleanHolder) {
    return when (x) {
        is True -> Unit
        else <!UNSUPPORTED_FEATURE!>if true<!> -> Unit
        else -> Unit
    }
}

fun falseNegativeNotExhaustive(x: Any) {
    val w = <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is Int <!UNSUPPORTED_FEATURE!>if { _ : String -> x == "10" } ("11")<!> -> println('q')
    }
}
