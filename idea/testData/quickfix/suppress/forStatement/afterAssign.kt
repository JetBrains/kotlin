// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo() {
    var x = 0
    [suppress("UNNECESSARY_NOT_NULL_ASSERTION")]
    (x = 1<caret>!!)
}