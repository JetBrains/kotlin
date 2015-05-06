// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"

@suppress("FOO")
fun foo(): String?<caret>? = null