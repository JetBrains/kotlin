@Suppress("NOTHING_TO_INLINE")
inline fun foo(i4: Int, i5: Int): Int {
    return i4 + i5
}

fun bar(i1: Int, i2: Int, i3: Int): Int {
    return foo(i1 + i2, i3)
}

fun main(args: Array<String>) {
    println(bar(1, 2, 3).toString())
}
