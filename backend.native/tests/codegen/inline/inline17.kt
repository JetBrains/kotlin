@Suppress("NOTHING_TO_INLINE")
inline fun <T> foo(i1: T, i2: T): List<T> {
    return listOf(i1, i2)
}

fun bar(): List<Int> {
    return foo <Int> (1, 2)
}

fun main(args: Array<String>) {
    println(bar().toString())
}
