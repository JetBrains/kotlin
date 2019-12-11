interface T {
    fun getX() = 1
}

class C : T {
    val x: Int
        get() = 1
}