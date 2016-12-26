@Suppress("NOTHING_TO_INLINE")
inline fun foo() {
    println("Ok")
}

fun bar(i: Int, j: Int): Int {
    foo()
    return i + j
}

fun main(args: Array<String>) {
    println(bar(1, 2).toString())
}

