@Suppress("NOTHING_TO_INLINE")
inline fun <T> foo (): Boolean {
    return Any() is Any
}

fun bar(i1: Int): Boolean {
    return foo<Double>()
}

fun main(args: Array<String>) {
    println(bar(1).toString())
}
