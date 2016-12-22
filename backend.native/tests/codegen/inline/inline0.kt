@Suppress("NOTHING_TO_INLINE")
inline fun foo(i: Int): Int {
    return i + 1
}

fun bar(i: Int): Int {
    return foo(i)
}

fun main(args: Array<String>) {
    println(bar(1).toString())
}

