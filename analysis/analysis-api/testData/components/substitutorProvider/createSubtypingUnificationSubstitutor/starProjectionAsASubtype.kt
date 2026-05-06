interface Container<A>

fun Container<Int>.containerExtension() {
    th<caret_1_target>is
}

fun usage(x: Container<*>) {
    <caret_1_base>x
}
