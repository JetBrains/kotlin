// LANGUAGE: +WhenGuards
// DIAGNOSTICS: -DUPLICATE_LABEL_IN_WHEN, -USELESS_IS_CHECK

fun <T> materialize(): T {
    TODO()
}

fun expectedTypeInGuard(x: Any) {
    when(x) {
        is Int <!UNSUPPORTED_FEATURE!>if materialize<Boolean>()<!> -> 100
        is String <!UNSUPPORTED_FEATURE!>if materialize()<!> -> 200
        is Double <!UNSUPPORTED_FEATURE!>if materialize<String>()<!> -> 100
        else -> 0
    }
}
