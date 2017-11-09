fun <R> /*rename*/foo(f: () -> R) = f()

fun test() {
    foo { return@foo false }

    foo(fun(): Boolean { return@foo false })
}