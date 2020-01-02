interface My {
    open fun foo()
    open fun bar() {}
    open abstract fun baz(): Int

    open val x: Int
    open val y: String
        get() = ""
    open abstract val z: Double
}
