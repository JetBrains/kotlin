fun doSomething<T>(a: T) {}

fun test() {
    class Test {
        fun invoke(a: Int, vararg b: String, fn: () -> Unit): String = "test"
    }
    val test = Test()
    doSomething(test.i<caret>nvoke(1, "a", "b") { })
}
