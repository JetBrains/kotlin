// "Suppress 'REDUNDANT_NULLABLE' for class D" "true"

class C {
    class D {
        fun foo(): String?<caret>? = null
    }
}