// "Suppress 'REDUNDANT_NULLABLE' for default object Default of C" "true"

class C {
    [suppress("REDUNDANT_NULLABLE")]
    default object {
        var foo: String?<caret>? = null
    }
}