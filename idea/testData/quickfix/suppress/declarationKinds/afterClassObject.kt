// "Suppress 'REDUNDANT_NULLABLE' for class object of C" "true"

class C {
    [suppress("REDUNDANT_NULLABLE")]
    class object {
        var foo: String?<caret>? = null
    }
}