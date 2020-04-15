// WITH_RUNTIME
fun test(foo: String, bar: Int) {
    val s = <caret>"aaa\nbbb$foo$bar"
}