// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -SENSELESS_COMPARISON, -USELESS_IS_CHECK, -USELESS_CAST, -DUPLICATE_LABEL_IN_WHEN

fun returnInGuard(x: Any): String {
    when(x) {
        true <!UNSUPPORTED_FEATURE!>if return ""<!> -> {}
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun returnInCondition(x: Any): String {
    when(x) {
        return ""<!UNREACHABLE_CODE!><!> <!UNSUPPORTED_FEATURE!>if return ""<!> -> <!UNREACHABLE_CODE!>{}<!>
    }
}

fun returnInBody(x: Any): String {
    when (x) {
        is String <!UNSUPPORTED_FEATURE!>if x == ""<!> -> return "" }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun exhaustiveWhen(x: Any): String {
    when(x) {
        true <!UNSUPPORTED_FEATURE!>if x == true<!> -> return ""
        false <!UNSUPPORTED_FEATURE!>if x == false<!> -> return ""
        else -> return ""
    }
}
