fun foo(f: (Int) -> Boolean) {
    f(1)
}
fun test() {
    foo(<caret>fun(i: Int): Boolean {return i > 0 })
}