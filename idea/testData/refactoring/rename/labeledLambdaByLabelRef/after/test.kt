fun <R> foo(f: () -> R) = f()

fun test() {
    foo baz@ { return@baz false }
}