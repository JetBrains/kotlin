// "Convert expression '1 + 3L' to 'Float'" "true"
fun foo() {
    bar(1 + 3L<caret>)
}

fun bar(l: Float) {
}