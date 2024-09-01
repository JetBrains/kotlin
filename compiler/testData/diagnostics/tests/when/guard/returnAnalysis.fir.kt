// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -SENSELESS_COMPARISON, -USELESS_IS_CHECK, -USELESS_CAST, -DUPLICATE_LABEL_IN_WHEN

fun returnInGuard(x: Any): String {
    when(x) {
        true if return "" -> {}
    }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun returnInCondition(x: Any): String {
    when(x) {
        return "" if return "" -> {}
    }
}

fun returnInBody(x: Any): String {
    when (x) {
        is String if x == "" -> return "" }
<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun exhaustiveWhen(x: Any): String {
    when(x) {
        true if x == true -> return ""
        false if x == false -> return ""
        else -> return ""
    }
}
