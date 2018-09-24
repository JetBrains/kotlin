fun foo() {}

fun test(b: Boolean) {
    <caret>while (b) /* aaa */ foo() // bbb
}
