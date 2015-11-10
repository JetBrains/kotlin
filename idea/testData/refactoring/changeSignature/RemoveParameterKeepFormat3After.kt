fun <caret>foo(a: Int, b: Int) {

}

fun test() {
    foo(1,
            2)
    foo(1, 2)
    foo(
            1,
            2
    )
}