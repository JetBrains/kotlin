fun <T> doSomething(a: T) {}

fun test() {
    class Test {
        operator fun invoke(a: Int, vararg b: String, fn: () -> Unit): String = "test"
    }
    val test = Test()
    doSomething(test.i<caret>nvoke(1, "a", "b") { })
}
