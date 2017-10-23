// PROBLEM: none

class Test(val test: Int) {
    fun foo() = test
}

// Receiver unused but still inapplicable (infix)
infix fun <caret>Test.build(x: Int) = Test(x * x)


fun main(args: Array<String>) {
    val x = Test(0) build 7
    x.foo()
}