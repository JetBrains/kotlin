interface Container<A>

fun usage(xx: Container<String>, yy: Container<*>) {
    x<caret_1_left>x
    y<caret_1_right>y
}
