interface X {
    infix fun plus(p: Int): Int
}

fun foo(x: X) {
    x.<caret>plus(1).minus(2)
}
