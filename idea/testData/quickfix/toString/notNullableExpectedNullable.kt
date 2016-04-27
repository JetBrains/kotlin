// "Add 'toString()' call" "true"
// ACTION: Add 'toString()' call

fun foo() {
    bar(Any()<caret>)
}

fun bar(a: String?) {
}