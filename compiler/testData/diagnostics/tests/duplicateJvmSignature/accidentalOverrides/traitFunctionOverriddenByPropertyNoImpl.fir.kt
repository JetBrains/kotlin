interface T {
    fun getX(): Int
}

abstract class C : T {
    val x: Int
        get() = 1
}