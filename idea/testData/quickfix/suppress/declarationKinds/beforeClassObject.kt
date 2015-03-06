// "Suppress 'REDUNDANT_NULLABLE' for default object Default of C" "true"

class C {
    default object {
        var foo: String?<caret>? = null
    }
}