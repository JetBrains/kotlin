interface Container<A>

fun Container<Int>.containerExtension() {
    th<caret_1_right>is
}

fun usage(x: Container<*>) {
    <caret_1_left>x
}
