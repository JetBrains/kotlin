class TestingUse {
    fun test4(printNum: (a: Int, b: String) -> Unit, c: Int): Int {
        printNum(c, "This number is")
        return c
    }
}

fun main() {
    val num = TestingUse().test4({ <caret>x, str -> }, 5)
}
