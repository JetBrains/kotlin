class Test {
    companion object {
        fun f(x: Int, y: Int) = 1
    }

    fun f(x: Int, y: String) = 2

    fun test() {
        <caret>Companion.f(1, 2)
    }
}