@Suppress("NOTHING_TO_INLINE")
inline fun bar(block: () -> String) : String {
    return block()
}

fun bar2() : String {
    return bar { return "def" }
}

fun main(args: Array<String>) {
    println(bar2())
}
