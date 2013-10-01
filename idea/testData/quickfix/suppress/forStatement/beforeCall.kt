// "Suppress 'REDUNDANT_NULLABLE' for statement " "true"

fun foo() {
    call("": String?<caret>?)
}

fun call(s: String?) {}