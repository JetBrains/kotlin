// "Suppress 'REDUNDANT_NULLABLE' for class C" "true"

@suppress("REDUNDANT_NULLABLE")
class C {
    fun foo(): String?<caret>? = null
}