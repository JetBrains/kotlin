fun String.outer(x: Int) {
    fun test1(x: Int, y: Int) {
        fun test2(x: Int) = this + x + y
        test2(y).length + x + y
    }
    test1(x, x * x)
}