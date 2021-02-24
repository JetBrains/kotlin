fun foo(p: String) {}

fun <T> bar(fn: (T) -> Unit) {}

fun test() {
    bar<String>(<caret>fun(x: String) {
        foo(x)
    })
}