fun foo(vararg x: String) {}

fun bar() {
    foo(<caret>*arrayOf("abc", "def"))
}
