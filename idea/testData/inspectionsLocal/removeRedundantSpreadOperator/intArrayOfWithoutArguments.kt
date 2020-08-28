fun foo(vararg args: Int) {}

fun test() {
    foo(<caret>*intArrayOf())
}