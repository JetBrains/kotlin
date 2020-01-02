open class B {
    val x: Int
        get() = 1
}

class C : B() {
    fun getX() = 1
}