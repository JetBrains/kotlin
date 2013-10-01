// "Suppress 'REDUNDANT_NULLABLE' for statement " "false"
// ACTION: Remove redundant '?'

fun foo() {
    any {
        (x: String?<caret>?) ->
    }
}

fun any(a: Any?) {}