// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN, -USELESS_IS_CHECK

sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

fun guardWithoutSubject(x: Any) {
    return when {
        <!EXPECTED_CONDITION!>!is String<!> -> Unit
        x is String -> Unit
        x is BooleanHolder <!WHEN_GUARD_WITHOUT_SUBJECT!>if x.value<!> -> Unit
        if (x is Boolean) true else false -> Unit
        else <!WHEN_GUARD_WITHOUT_SUBJECT!>if x is Boolean<!> -> Unit
        else -> Unit
    }
}
