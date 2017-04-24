fun <R> foo(f: () -> R) = f()

fun test() {
    foo /*rename*/foo@ { return@foo false }
}