// "Suppress 'REDUNDANT_NULLABLE' for class C" "true"

@suppress("REDUNDANT_NULLABLE")
class C {
    class D {
        fun foo(): String?<caret>? = null
    }
}