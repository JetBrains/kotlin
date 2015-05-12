// INTENTION_TEXT: Replace with '..' operator
fun test() {
    class Test {
        fun rangeTo(a: Int): Test = Test()
    }
    val test = Test()
    test.rangeTo<caret>(1)
}
