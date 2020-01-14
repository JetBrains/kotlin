fun <caret>foo(a: Int, b: Int, d: Int, c: Int, e: Int) {

}

fun test() {
    foo(
            1,
            2,
            4,
            3,
            5
    )
    foo(
            1, 2,
            4,
            3,
            5
    )
    foo(
            1,
            2,
            4,
            3,
            5
    )
}