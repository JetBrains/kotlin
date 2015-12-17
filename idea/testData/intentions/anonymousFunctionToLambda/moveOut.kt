fun foo(f: () -> Unit, i: Int) {
    f()
}

fun main(args: String) {
    foo(<caret>fun() {
        val p = 1
    }, 1)
}