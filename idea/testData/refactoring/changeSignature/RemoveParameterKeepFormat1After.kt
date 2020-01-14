fun <caret>foo(b: Int, c: Int) {

}

fun test() {
    foo(
            2,
            3
    )
    foo(2, 3)
    foo(
            2,
            3
    )
}