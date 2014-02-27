fun test() {
    class Test{
        fun get(a: Int, b: Int, c: Int = 1, d: Int = 1 fn: (i: Int) -> Int) : Int = 0
    }
    val test = Test()
    test.g<caret>et(1, c=3, b=2) { i ->
        i
    }
}
