// "Suppress 'REDUNDANT_NULLABLE' for class object of C" "true"

class C {
    class object {
        var foo: String?<caret>? = null
    }
}