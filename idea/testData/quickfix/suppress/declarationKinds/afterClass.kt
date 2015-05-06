// "Suppress 'REDUNDANT_NULLABLE' for class C" "true"

@suppress("REDUNDANT_NULLABLE")
class C {
    var foo: String?<caret>? = null
}