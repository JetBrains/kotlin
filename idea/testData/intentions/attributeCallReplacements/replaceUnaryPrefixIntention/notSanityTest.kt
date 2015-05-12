// INTENTION_TEXT: Replace with '!' operator
fun test() {
    class Test {
        fun not(): Test = Test()
    }
    val test = Test()
    test.n<caret>ot()
}
