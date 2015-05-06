// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"

@suppress("FOO", "REDUNDANT_NULLABLE")
fun foo(): String?<caret>? = null