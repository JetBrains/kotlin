// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "false"

[suppress("REDUNDANT_NULLABLE")]
fun foo(): String?<caret>? = null