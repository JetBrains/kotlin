// IS_APPLICABLE: false
fun foo() {
    f(
        1,
        2,
        g<caret>(3, 4, 5),
        3
    )
}

fun f(a: Int, b: Int, c: Int, d: Int): Int = 0

fun g(a: Int, b: Int, c: Int): Int = 0