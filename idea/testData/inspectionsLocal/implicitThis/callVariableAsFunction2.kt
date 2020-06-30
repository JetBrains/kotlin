class Bar {
    val bar: () -> Unit = {}
}

fun Bar.test() {
    <caret>bar()
}
