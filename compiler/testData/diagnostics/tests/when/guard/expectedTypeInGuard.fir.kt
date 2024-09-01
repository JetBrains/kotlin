// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN, -USELESS_IS_CHECK

fun <T> materialize(): T {
    TODO()
}

fun expectedTypeInGuard(x: Any) {
    when(x) {
        is Int if materialize<Boolean>() -> 100
        is String if materialize() -> 200
        is Double if <!CONDITION_TYPE_MISMATCH, TYPE_MISMATCH!>materialize<String>()<!> -> 100
        else -> 0
    }
}
