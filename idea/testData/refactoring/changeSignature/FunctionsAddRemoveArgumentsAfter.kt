fun foo(x0: Any?,
        x1: Int = 1,
        x2: Float) {
    foo(null, 2, 3.5);
    val y1 = x1;
    val y2 = x2;
    val y3 = x3;
}

fun bar() {
    foo(null, x1 = 2, x2 = 3.5);
}
