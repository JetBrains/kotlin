// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"

fun foo(a: Array<Int>) {
    @suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    a[1<caret>!!]
}