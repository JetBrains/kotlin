fun <R> foo(f: () -> R) = f()

fun test() {
    foo (bar2@ baz@ { return@baz false })
}