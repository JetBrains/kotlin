@Suppress("NOTHING_TO_INLINE")
inline fun foo(i4: Int, i5: Int) {
    println("hello $i4 $i5")
}

fun bar(i1: Int, i2: Int) {
    foo(i1, i2)
}

fun main(args: Array<String>) {
    bar(1, 8)
}
