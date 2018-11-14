fun foo() {}

fun test(b: Boolean) {
    <caret>if (b) foo() // bbb
    // ccc
}