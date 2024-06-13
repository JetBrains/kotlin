// DIAGNOSTICS: -UNUSED_VARIABLE

fun <R> myRun(b: () -> R): R = b()

fun <T> materialize(): T = TODO()

fun foo(x: String?) {
    val r = myRun {
        val y = x ?: return@myRun materialize()
        y.length
    }

    r.minus(1)
}
