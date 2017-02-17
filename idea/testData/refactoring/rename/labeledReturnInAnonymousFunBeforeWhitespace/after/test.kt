fun <R> bar(f: () -> R) = f()

fun test() {
    bar(fun(): Boolean { return@bar false })
}