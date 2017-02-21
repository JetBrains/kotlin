@Suppress("NOTHING_TO_INLINE")
inline fun foo2(i1: Int): Int {
    return i1 + 1
}

@Suppress("NOTHING_TO_INLINE")
inline fun foo1(i1: Int, body: (Int) -> Int): Int {
    return body(i1)
}

fun bar(i0: Int): Int {
    return foo1(i0) { foo2(it) + 1 }
}

fun main(args: Array<String>) {
    println(bar(2).toString())
}
