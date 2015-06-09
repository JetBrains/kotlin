// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"

@ann fun foo(): String?<caret>? = null

annotation class ann