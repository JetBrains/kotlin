fun test() {
    class Test {
        fun invoke(a: Int, vararg b: String) {}
    }
    val test = Test()
    test.i<caret>nvoke(1, "a", "b")
}
