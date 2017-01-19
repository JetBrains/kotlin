// @Suppress("NOTHING_TO_INLINE")
fun foo(i4: Int, i5: Int): Int {
    try {
        return i4 / i5
    } catch (e: Exception) {
        return i4
    }
}

fun bar(i1: Int, i2: Int, i3: Int): Int {
    return i1 + foo(i2, i3)
}

fun main(args: Array<String>) {
    println(bar(1, 8, 0).toString())
}
