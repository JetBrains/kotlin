// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -SENSELESS_COMPARISON, -USELESS_IS_CHECK, -USELESS_CAST, -DUPLICATE_LABEL_IN_WHEN

class NullableStringHolder(val value: String?)
sealed class BooleanHolder(val value: Boolean)
object True : BooleanHolder(true)
object False : BooleanHolder(false)

fun smartCastInGuard(x: Any) {
    when (x) {
        is String -> Unit
        is BooleanHolder <!UNSUPPORTED_FEATURE!>if x.value<!> -> Unit
        is NullableStringHolder <!UNSUPPORTED_FEATURE!>if x.value != null && x.value.length > 0<!> -> Unit
        is NullableStringHolder <!UNSUPPORTED_FEATURE!>if {x.value != null && x.value.length > 0}()<!> -> Unit
        is NullableStringHolder <!UNSUPPORTED_FEATURE!>if x.value != null<!> -> <!DEBUG_INFO_SMARTCAST!>x<!>.value<!UNSAFE_CALL!>.<!>length
        is NullableStringHolder <!UNSUPPORTED_FEATURE!>if run {x.value != null}<!> -> <!DEBUG_INFO_SMARTCAST!>x<!>.value<!UNSAFE_CALL!>.<!>length
        else <!UNSUPPORTED_FEATURE!>if x is Boolean && x<!> -> Unit
        else -> Unit
    }
}

fun failingSmartCastInWhen(x: Any, y: String) {
    when (x) {
        "test" <!UNSUPPORTED_FEATURE!>if x.length > 0<!> -> Unit
        y <!UNSUPPORTED_FEATURE!>if x.length<!> -> Unit
        "test" -> x.<!UNRESOLVED_REFERENCE!>length<!>
        y -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
