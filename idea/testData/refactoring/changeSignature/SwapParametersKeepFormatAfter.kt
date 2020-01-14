fun <caret>foo(c: Int, b: Int, a: Int) {

}

fun test() {
    foo(
            3,
            2,
            1
    )
    foo(
            3, 2,
            1
    )
    foo(
            3,
            2,
            1
    )
}