// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME

// KT-37163
// Case that was found in kotlinx.coroutines

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

fun box(): String {
    return "OK"
}
