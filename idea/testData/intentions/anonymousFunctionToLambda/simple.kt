fun foo(f: () -> Unit) {
    f()
}

fun main(args: String) {
    foo(fun<caret>() {
        val p = 1
    })
}