class Foo {
    val x: Int
        get() = 42

    @Suppress("CONFLICTING_JVM_DECLARATIONS")
    fun getX() = 42

    @JvmName("getX")
    fun getY() = 42
}