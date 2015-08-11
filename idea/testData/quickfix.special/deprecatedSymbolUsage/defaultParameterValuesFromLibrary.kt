// "Replace with 'prefix + joinTo(StringBuilder(), separator, "", postfix, limit, truncated, transform)'" "true"

fun foo() {
    listOf(1, 2, 3).<caret>joinToString(limit = 10)
}