fun test() {
    class Test {
        fun invoke(a: Int, vararg b: String, fn: () -> Unit): String = "test"
    }
    val test = Test()
    println(test.i<caret>nvoke(1, "a", "b") { })
}
