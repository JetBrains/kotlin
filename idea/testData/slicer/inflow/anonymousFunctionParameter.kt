// FLOW: IN

fun foo(f: (Int) -> Unit) {
    f(1)
}

fun test() {
    foo(fun(value: Int) {
        val <caret>v = value
    })
}