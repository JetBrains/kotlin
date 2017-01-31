@Suppress("NOTHING_TO_INLINE")
inline fun foo(i2: Int, body: () -> Int): Int {
    return i2 + body()
}

fun bar(i1: Int): Int {
    return foo(i1) { 1 }
}

fun main(args: Array<String>) {
    println(bar(1).toString())
}
