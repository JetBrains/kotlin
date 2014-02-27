fun test() {
    class Test {
        fun invoke(a: Int, b: String) {}
    }
    val test = Test()
    test.i<caret>nvoke(1, "s")
}
