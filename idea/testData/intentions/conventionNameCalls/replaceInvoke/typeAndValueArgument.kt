fun test() {
    class Test {
        fun <T> invoke(a: Int) {}
    }
    val test = Test()
    test.i<caret>nvoke<Int>(0)
}
