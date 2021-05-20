// "Wrap with '?.let { ... }' call" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION
// ERROR: Type mismatch: inferred type is String? but String was expected
// WITH_RUNTIME

fun test(s: String?): String {
    while (true) notNull(notNull(<caret>s))
}

fun notNull(name: String): String = name