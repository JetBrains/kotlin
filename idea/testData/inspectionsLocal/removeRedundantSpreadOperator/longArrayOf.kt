fun foo(vararg x: Long) {}

fun bar() {
    foo(<caret>*longArrayOf(1L))
}
