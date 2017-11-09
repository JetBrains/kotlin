fun <caret>foo() = 1

fun bar() {
    val x = fun () {
        val y = foo()
    }
}

fun baz() {
    bar()
}