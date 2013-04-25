fun foo(<caret>x1: Int = 1, x2: Float, x3: ((Int) -> Int)?) {
    foo(2, 3.5, null);
    val y1 = x1;
    val y2 = x2;
    val y3 = x3;
}

fun bar() {
    foo(x1 = 2, x2 = 3.5, x3 = null);
}
