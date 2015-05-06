// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"

@suppress("REDUNDANT_NULLABLE")
fun foo() {
    val a: String?<caret>? = null
}
