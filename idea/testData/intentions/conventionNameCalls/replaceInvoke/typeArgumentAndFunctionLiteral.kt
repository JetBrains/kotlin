fun test() {
    class Test {
        fun invoke<T>(fn: () -> T) {}
    }
    val test = Test()
    test.i<caret>nvoke<Int> { 0 }
}
