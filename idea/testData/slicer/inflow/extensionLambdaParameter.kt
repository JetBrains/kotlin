// FLOW: IN
// RUNTIME_WITH_SOURCES

fun foo(f: String.(Int) -> Unit) {
    f("", 1)

    "".f(2)

    with("") {
        f(3)
    }
}

fun test() {
    foo { i ->
        val v = <caret>i
    }
}
