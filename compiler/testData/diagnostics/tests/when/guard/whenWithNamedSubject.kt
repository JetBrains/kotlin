// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN, -USELESS_IS_CHECK

sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

fun <T> supply(v: T): T {
    return v
}

fun whenWithNamedSubject(y: BooleanHolder) {
    when (val x = if (y.value) y else supply(True)) {
        is True <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        is False <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        is <!INCOMPATIBLE_TYPES!>String<!> <!UNSUPPORTED_FEATURE!>if x.length == 0<!> -> Unit
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> Unit
    }

    when (val x = y) {
        is True <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        is False <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> Unit
    }
}
