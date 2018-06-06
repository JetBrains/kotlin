class Test(val str: String) {
    private fun <caret>Test.print() {
        println(str)
        println(this.str)
        println(this@print.str)
        println(this)
    }

    fun test(test: Test) {
        test.print()
        Test("three").print()
    }
}

fun main(args: Array<String>) {
    Test("one").test(Test("two"))
}

fun println(a: Any) {}
