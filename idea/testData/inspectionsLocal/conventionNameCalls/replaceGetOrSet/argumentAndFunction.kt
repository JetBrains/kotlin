fun test() {
    class Test{
        operator fun get(a: Int, b: Int, fn: (i: Int) -> Int) : Int = 0
    }
    val test = Test()
    test.g<caret>et(1, 2) { i ->
        i
    }
}

fun withSuppression() {
    class Test{
        operator fun get(a: Int, b: Int, fn: (i: Int) -> Int) : Int = 0
    }

    val test = Test()

    @Suppress("ReplaceGetOrSet")
    test.get(1, 2) { i -> i }
}
