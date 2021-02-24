fun foo(p: String) {}

fun <T> bar(fn: (T) -> Unit) {}

fun test() {
    bar(<caret>fun(x: String) {
        foo(x)
    })
}