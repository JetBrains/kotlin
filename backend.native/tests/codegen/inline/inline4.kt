@Suppress("NOTHING_TO_INLINE")
inline fun foo(i4: Int, i5: Int): Int {
    if (i4 > 0) return i4
    return i5
}

fun bar(i1: Int, i2: Int): Int {
    return foo(i1, i2)
}

fun main(args: Array<String>) {
    println(bar(3, 8).toString())
}
