// INTENTION_TEXT: Replace with '-' operator
fun test() {
    class Test {
        fun minus(): Test = Test()
    }
    val test = Test()
    test.min<caret>us()
}
