// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -SENSELESS_COMPARISON, -USELESS_IS_CHECK, -USELESS_CAST, -DUPLICATE_LABEL_IN_WHEN

class NullableStringHolder(val value: String?)
sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

fun smartCastInGuard(x: Any) {
    when (x) {
        is String -> Unit
        is BooleanHolder if x.value -> Unit
        is NullableStringHolder if x.value != null && x.value.length > 0 -> Unit
        is NullableStringHolder if {x.value != null && x.value.length > 0}() -> Unit
        is NullableStringHolder if x.value != null -> x.value.length
        is NullableStringHolder if run {x.value != null} -> x.value<!UNSAFE_CALL!>.<!>length
        else if x is Boolean && x -> Unit
        else -> Unit
    }
}

fun failingSmartCastInWhen(x: Any, y: String) {
    when (x) {
        "test" if x.<!UNRESOLVED_REFERENCE!>length<!> > 0 -> Unit
        y if x.<!UNRESOLVED_REFERENCE!>length<!> -> Unit
        "test" -> x.<!UNRESOLVED_REFERENCE!>length<!>
        y -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
