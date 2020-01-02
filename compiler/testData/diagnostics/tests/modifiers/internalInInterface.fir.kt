interface My {
    internal val x: Int
    internal val xxx: Int
        get() = 0
    internal fun foo(): Int
    internal fun bar() = 42
}
