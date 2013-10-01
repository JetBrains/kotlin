// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo() {
    when (""<caret>!!) {
        is Any -> {}
    }
}