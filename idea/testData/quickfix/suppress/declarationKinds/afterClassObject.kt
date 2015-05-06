// "Suppress 'REDUNDANT_NULLABLE' for companion object Companion of C" "true"

class C {
    @suppress("REDUNDANT_NULLABLE")
    companion object {
        var foo: String?<caret>? = null
    }
}