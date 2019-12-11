interface My {
    private val x: Int
    private abstract val xx: Int
    private val xxx: Int
        get() = 0
    final val y: Int
    final val yy: Int
        get() = 1
    private fun foo(): Int
    // ok
    private fun bar() = 42
}
