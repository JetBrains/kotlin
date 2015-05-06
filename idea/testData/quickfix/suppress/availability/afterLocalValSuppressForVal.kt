// "Suppress 'REDUNDANT_NULLABLE' for val a" "true"

fun foo() {
    @suppress("REDUNDANT_NULLABLE")
    val a: String?<caret>? = null
}
