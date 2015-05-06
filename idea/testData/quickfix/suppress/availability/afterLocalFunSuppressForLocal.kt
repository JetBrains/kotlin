// "Suppress 'REDUNDANT_NULLABLE' for fun local" "true"

fun foo() {
    @suppress("REDUNDANT_NULLABLE")
    fun local(): String?<caret>? = null
}
