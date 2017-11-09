fun foo(vararg x: Char) {}

fun bar() {
    foo(*<caret>charArrayOf('a', 'b'))
}
