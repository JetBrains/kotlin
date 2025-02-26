private const val UNUSED = 100

inline fun calculate(): Int {
    val lambda = {
        40 + 2
    }
    return lambda()
}
