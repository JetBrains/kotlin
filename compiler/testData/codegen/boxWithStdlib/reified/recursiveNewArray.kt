import kotlin.InlineOption.*

inline fun<reified T> createArray(n: Int, inlineOptions(ONLY_LOCAL_RETURN) block: () -> T): Array<T> {
    return Array<T>(n) { block() }
}

inline fun<T1, T2, T3, T4, T5, T6, reified R> recursive(
        inlineOptions(ONLY_LOCAL_RETURN) block: () -> R
): Array<R> {
    return createArray(5) { block() }
}

fun box(): String {
    val x = recursive<Int, Int, Int, Int, Int, Int, String>(){ "abc" }

    assert(x.all { it == "abc" })
    return "OK"
}
