fun foo(y: Boolean) {
    val x = 3
    <caret>x != x && (2 > 1 || y)
}