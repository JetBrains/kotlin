fun foo(vararg x: Char) {}

fun bar() {
    foo(*charArrayOf<caret>('a', 'b'))
}
