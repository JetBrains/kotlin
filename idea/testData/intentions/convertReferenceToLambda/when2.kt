class Test {
    fun bar() = 1

    fun test(x: Int) {
        val foo: () -> Int = when (x) {
            1 -> this::bar
            else -> <caret>this::bar
        }
    }
}