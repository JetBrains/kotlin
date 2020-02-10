open class B {
    private fun getX() = 1
}

class C : B() {
    val x: Int
        get() = 1
}