// SHOULD_FAIL_WITH: vararg.not.last
fun test() {
    class Test{
        fun contains(vararg b: Int, c: Int = 0): Boolean = true
    }
    val test = Test()
    test.contains<caret>(c=5)
}
