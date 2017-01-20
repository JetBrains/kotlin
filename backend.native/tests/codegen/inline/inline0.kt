@Suppress("NOTHING_TO_INLINE")
inline fun foo(i1: Int, j1: Int): Int {
    return i1 + j1
}

fun bar(i: Int, j: Int): Int {
    return i + foo(i, j)
}

fun main(args: Array<String>) {
    println(bar(41, 2).toString())
}

