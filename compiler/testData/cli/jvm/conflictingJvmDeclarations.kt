class Foo {
    val x: Int
        get() = 42

    fun getX() = 42

    @JvmName("getX")
    fun getY() = 42
}