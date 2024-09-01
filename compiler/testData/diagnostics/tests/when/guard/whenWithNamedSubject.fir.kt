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
        is True if x.value -> Unit
        is False if x.value -> Unit
        is String if x.length == 0 -> Unit
        else -> Unit
    }

    when (val x = y) {
        is True if x.value -> Unit
        is False if x.value -> Unit
        else -> Unit
    }
}
