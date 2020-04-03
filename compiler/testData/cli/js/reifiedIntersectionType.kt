fun test1() {
    val flow = combine(
        flowOf("1"),
        flowOf(2)
    ) { arr -> arr.joinToString() }
}

fun <T> Array<out T>.joinToString(): String = ""

public inline fun <reified T, R> combine(
    vararg flows: Flow<T>,
    crossinline transform: suspend (Array<T>) -> R
): Flow<R> = TODO()

fun <T> flowOf(value: T): Flow<T> = TODO()
interface Flow<out T>