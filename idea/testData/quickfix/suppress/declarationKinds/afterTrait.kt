// "Suppress 'REDUNDANT_NULLABLE' for trait C" "true"

@suppress("REDUNDANT_NULLABLE")
trait C {
    var foo: String?<caret>?
}