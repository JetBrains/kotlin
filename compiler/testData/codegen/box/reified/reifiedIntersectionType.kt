// IGNORE_BACKEND: JS
// KJS_WITH_FULL_RUNTIME

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

// Simplified case

class In<in T>

inline fun <reified K> select(x: K, y: K): K = x
interface A
interface B

fun test2(a: In<A>, b: In<B>) {
    select(a, b)
}

fun box(): String {
    return "OK"
}
