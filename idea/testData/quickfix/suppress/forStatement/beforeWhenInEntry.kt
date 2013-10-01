// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo() {
    when (1) {
        in 1<caret>!!..2 -> {}
    }
}