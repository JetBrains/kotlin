@ExperimentalStdlibApi
fun test(s: ArrayDeque<Int>): ArrayDeque<Int>? {
    ArrayDeque<Int>(42)

    val x: ArrayDeque<Int>? = null
    return x ?: s
}
