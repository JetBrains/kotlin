// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN, -USELESS_IS_CHECK

sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

fun guardWithoutSubject(x: Any) {
    return when {
        <!EXPECTED_CONDITION!>!is String<!> -> Unit
        x is String -> Unit
        x is BooleanHolder <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        if (x is Boolean) true else false -> Unit
        else <!UNSUPPORTED_FEATURE!>if x is Boolean<!> -> Unit
        else -> Unit
    }
}
