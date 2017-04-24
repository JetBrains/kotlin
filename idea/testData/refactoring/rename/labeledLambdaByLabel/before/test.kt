fun <R> foo(f: () -> R) = f()

fun test() {
    foo /*rename*/bar@ { return@bar false }
}