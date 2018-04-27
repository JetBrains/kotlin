// WITH_RUNTIME

class Test(val str: String) {

    private fun <caret>Test.print() {
        println(str)
        println(this.str)
        println(this@print.str)
        println()
    }

    fun test(test: Test) {
        print()
        test.print()
        Test("three").print()
    }
}

fun main(args: Array<String>) {
    Test("one").test(Test("two"))
}
