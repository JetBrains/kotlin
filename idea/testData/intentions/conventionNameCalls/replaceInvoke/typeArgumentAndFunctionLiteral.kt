fun test() {
    class Test {
        fun <T> invoke(fn: () -> T) {}
    }
    val test = Test()
    test.i<caret>nvoke<Int> { 0 }
}
