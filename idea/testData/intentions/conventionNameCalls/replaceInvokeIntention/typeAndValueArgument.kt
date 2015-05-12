fun test() {
    class Test {
        fun invoke<T>(a: Int) {}
    }
    val test = Test()
    test.i<caret>nvoke<Int>(0)
}
