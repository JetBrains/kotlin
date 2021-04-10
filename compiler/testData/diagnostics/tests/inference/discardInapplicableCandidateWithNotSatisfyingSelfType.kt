// FIR_IDENTICAL
// WITH_RUNTIME

interface WithChildren<out T>

fun <T : WithChildren<*>> WithChildren<WithChildren<*>>.test() {
    withDescendants()
}

fun <T : WithChildren<T>> T.withDescendants() {}

@JvmName("foo")
fun WithChildren<*>.withDescendants() {}