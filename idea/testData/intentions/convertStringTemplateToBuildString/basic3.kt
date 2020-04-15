// WITH_RUNTIME
fun test(foo: String, bar: Int) {
    val s = <caret>"${foo}${bar}aaa\nbbb"
}