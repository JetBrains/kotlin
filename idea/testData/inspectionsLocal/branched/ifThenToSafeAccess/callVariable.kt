// HIGHLIGHT: INFORMATION
fun test(foo: (() -> Unit)?) {
    <caret>if (foo != null) foo()
}