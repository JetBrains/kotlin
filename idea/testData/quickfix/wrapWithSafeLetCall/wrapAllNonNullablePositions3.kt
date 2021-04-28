// "Wrap with '?.let { ... }' call" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION
// WITH_RUNTIME

fun test(s: String?) {
    notNull(notNull(<caret>s))
}

fun notNull(name: String): String = name