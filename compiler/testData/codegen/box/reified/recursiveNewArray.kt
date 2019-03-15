// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

inline fun<reified T> createArray(n: Int, crossinline block: () -> T): Array<T> {
    return Array<T>(n) { block() }
}

inline fun<T1, T2, T3, T4, T5, T6, reified R> recursive(
        crossinline block: () -> R
): Array<R> {
    return createArray(5) { block() }
}

fun box(): String {
    val x = recursive<Int, Int, Int, Int, Int, Int, String>(){ "abc" }

    assert(x.all { it == "abc" })
    return "OK"
}
