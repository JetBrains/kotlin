// "Suppress 'REDUNDANT_NULLABLE' for parameter p" "true"

fun foo(@suppress("REDUNDANT_NULLABLE") vararg p: String?<caret>?) = null