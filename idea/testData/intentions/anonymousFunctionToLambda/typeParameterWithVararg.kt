fun foo(p: String) {}

fun <T> bar(vararg fn: (T) -> Unit) {}

fun test() {
    bar(<caret>fun(x: String) {
        foo(x)
    })
}