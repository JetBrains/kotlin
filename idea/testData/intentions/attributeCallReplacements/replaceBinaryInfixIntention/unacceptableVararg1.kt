// SHOULD_FAIL_WITH: vararg.not.last
fun test() {
    class Test{
        fun plus(vararg b: Int, c: Int = 0): Int = 0
    }
    val test = Test()
    test.plus<caret>(c=5)
}
