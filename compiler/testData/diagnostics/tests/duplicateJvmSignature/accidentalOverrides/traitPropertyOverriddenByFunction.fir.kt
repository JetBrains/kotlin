interface T {
    val x: Int
        get() = 1
}

class C : T {
    fun getX() = 1
}