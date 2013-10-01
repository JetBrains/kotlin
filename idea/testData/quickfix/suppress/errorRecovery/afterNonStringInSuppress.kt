// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"
// ERROR: An integer literal does not conform to the expected type jet.String
// ERROR: An integer literal does not conform to the expected type jet.String

[suppress(1, "REDUNDANT_NULLABLE")]
fun foo(): String?<caret>? = null