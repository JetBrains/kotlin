// "Convert expression '1L' to 'Int'" "true"
fun foo() {
    bar(1L<caret>)
}

fun bar(l: Int) {
}