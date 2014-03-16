// INTENTION_TEXT: Replace with '-' operator
fun test() {
    class Test {
        fun minus(a: Int): Test = Test()
    }
    val test = Test()
    test.min<caret>us(1)
}
