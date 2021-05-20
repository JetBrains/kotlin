// "Wrap with '?.let { ... }' call" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION
// ERROR: A 'return' expression required in a function with a block body ('{...}')
// ERROR: Type mismatch: inferred type is String? but String was expected
// WITH_RUNTIME

fun test(s: String?): String? {
    if (true) {
        notNull(notNull(<caret>s))
    }
}

fun notNull(name: String): String = name