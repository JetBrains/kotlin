fun foo() {
    f(
        1,
        2,
        g(3, 4, 5),
        3
    )<caret>
}

fun f(a: Int, b: Int, c: Int, d: Int): Int = 0

fun g(a: Int, b: Int, c: Int): Int = 0