fun foo() {}

fun test(a: Int) {
    when (a) {
        <caret>1 -> /* aaa */ foo() // bbb
    }
}
