// "Wrap with '?.let { ... }' call" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION
// WITH_RUNTIME

fun test(s: String?) {
    nullable(nullable(notNull(notNull(<caret>s))))
}

fun notNull(name: String): String = name
fun nullable(name: String?): String = ""