// FLOW: IN

fun foo(f: (Int) -> Unit) {
    f(1)
}

fun test() {
    foo {
        val v = <caret>it
    }
}