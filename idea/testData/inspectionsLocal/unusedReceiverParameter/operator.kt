// PROBLEM: none

class Test(val test: Int) {
    fun foo() = test
}

// Receiver unused but still inapplicable (operator!)
operator fun <caret>Test.invoke(x: Int, y: Int) = Test(x + y)


fun main(args: Array<String>) {
    val x = Test(0)(1, 2)
    x.foo()
}