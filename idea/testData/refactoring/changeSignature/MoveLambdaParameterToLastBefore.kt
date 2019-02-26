fun <caret>foo(x: () -> Unit, y: Int) {
}

fun test() {
    foo({ 2 }, 1)
}