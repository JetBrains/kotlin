class Test {
    fun bar(a: String) = 1

    fun test(x: Int) {
        val foo: (a: String) -> Int = if (x == 1) this::bar else <caret>this::bar
    }
}