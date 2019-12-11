interface T {
    fun getX() = 1
}

interface C : T {
    val x: Int
        get() = 1
}
