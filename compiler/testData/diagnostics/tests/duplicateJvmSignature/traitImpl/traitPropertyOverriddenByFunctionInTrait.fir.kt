interface T {
    val x: Int
        get() = 1
}

interface C : T {
    fun getX() = 1
}
